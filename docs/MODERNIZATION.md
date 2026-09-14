# ACG Player X 现代化说明

## 1. 原项目审计结果

原工程最后一次提交停留在 2020 年，活动构建链路包含 Kotlin 1.3.72、Android Gradle Plugin 4.0、Gradle 6.1.1、compile/target SDK 29、JDK 8、Groovy 构建脚本和 `jcenter()`。界面层主要由 Java Activity/Fragment、XML、ButterKnife、SlidingUpPanel、RealtimeBlurView 与自定义主题辅助库组成。

主要风险包括：

- Fabric Crashlytics、Kotlin Android Extensions、`lifecycle-extensions` 等已淘汰组件
- 旧存储权限与 `requestLegacyExternalStorage`
- 单个 `MainActivity` 承担导航、权限、Live2D、网络检查、弹窗和页面切换
- 播放 UI 与 Fragment/第三方滑动面板强耦合
- 旧歌词页依赖硬编码在线接口，LRC 解析器主体实际上未完成
- 收藏、历史和桌面快捷入口依赖旧数据库、Activity 路由与服务状态
- 依赖版本散落且部分来自 JCenter/JitPack
- 仓库跟踪签名文件、Firebase 配置和 AAB 构建产物

## 2. 新构建基线

- Kotlin DSL 与 Version Catalog
- AGP 9.4.0 / Gradle 9.6.0 / JDK 17
- Kotlin 2.4.20 与 Compose Compiler Gradle plugin
- compileSdk / targetSdk 37，minSdk 23
- Compose BOM 2026.08.00
- 仅使用 Google Maven 与 Maven Central
- 非传递、非 final R 类
- Gradle build cache、configuration cache 与并行构建

逻辑模块名继续保持 `:app`，实际目录改为 `modern-app/`。旧 `app/` 与 `appthemehelper/` 不再被 `settings.gradle.kts` include，因此不会污染新构建，但仍可用于逐项核对旧行为。

## 3. 新架构

```text
MainActivity
  ├── publish dynamic launcher shortcuts
  ├── route cold/warm shortcut intents
  └── AcgPlayerApp (Compose responsive shell)
        ├── LibraryScreen
        │     ├── songs / favorites / recent
        │     ├── albums / artists / folders
        │     └── search and collection filters
        ├── NowPlayingScreen
        │     ├── cover pane
        │     ├── synchronized lyrics pane
        │     └── editable queue pane
        └── SettingsScreen

MainViewModel
  ├── MainUiState.destination
  ├── MusicRepository
  │     ├── MediaStore query + ContentObserver
  │     ├── DocumentTreeMusicScanner
  │     ├── cross-source merge/deduplication
  │     └── in-process SAF scan cache
  ├── AuthorizedFolderRepository
  │     ├── OpenDocumentTree persisted read grants
  │     └── DataStore directory registry
  ├── LibraryStateRepository
  │     ├── favorite media IDs
  │     └── bounded recent playback order
  ├── LyricsRepository
  │     ├── pure Kotlin LRC parser
  │     ├── internal per-media cache
  │     └── per-song user offset
  ├── SettingsRepository (DataStore)
  └── PlayerConnection (MediaController)
                           └── PlaybackService
                                 ├── MediaSession + ExoPlayer
                                 └── PlaybackStateStore
```

### 状态管理与导航

`MainViewModel` 暴露单一 `StateFlow<MainUiState>`。音乐库、MediaStore 权限、授权目录、扫描警告、查询、集合筛选、收藏、最近播放、主题、播放状态、当前歌词和顶层目标页面均以不可变状态驱动 Compose。

顶层导航不再只存在于 Composable 的临时状态，而是由 `MainUiState.destination` 驱动。`MainActivity` 在冷启动 `onCreate` 和热启动 `onNewIntent` 中统一解析 Shortcut action，使桌面长按“收藏”或“最近播放”能够可靠切换到音乐库对应分区，同时清理不相关的查询和集合过滤。

### 音乐来源链路

- MediaStore 使用分版本查询：Android 10+ 读取 `RELATIVE_PATH`，旧系统从 `DATA` 派生父目录
- `AuthorizedFolderRepository` 保存用户选择的树 URI，并取得/释放持久只读权限
- `DocumentTreeMusicScanner` 使用 `DocumentsContract` 广度优先遍历目录
- 单目录设置 20,000 项、32 层深度上限，并检测已访问目录 ID，防止异常提供程序循环
- 音频判断同时参考 MIME 和扩展名；元数据通过 `MediaMetadataRetriever` 读取
- 文档 URI 通过 SHA-256 生成稳定负数媒体 ID，与 MediaStore 正数 ID 分离
- SAF 路径内部包含稳定来源标识，UI 和搜索只展示可读目录路径
- MediaStore 与 SAF 合并时优先 MediaStore，并按 URI 与元数据指纹去重
- SAF 扫描结果使用进程内缓存；MediaStore 变更不会重复扫描所有文档树，手动刷新会强制重扫
- 权限失效、提供程序异常、深度或数量截断作为非阻断 warning 展示

### 收藏与最近播放链路

- `LibraryStateRepository` 使用 Preferences DataStore 保存收藏媒体 ID 集合和最近播放有序列表
- MediaStore 正数 ID 与 SAF 稳定负数 ID 使用同一协议，不依赖文件绝对路径
- 收藏切换为幂等集合操作，来源暂时不可用时不破坏记录
- 最近播放只在 `PlaybackUiState.isPlaying` 为真且媒体发生切换时写入，避免恢复暂停队列产生虚假历史
- 同一媒体再次播放会移动到首位并去重，列表上限为 100
- 展示最近播放时按持久顺序解析当前音乐库；不可用项被忽略但不破坏性删除
- 收藏和最近播放都支持现有全文搜索，并以当前可见歌曲作为播放队列
- 最近播放提供显式清空入口

### 播放与队列链路

- `PlaybackService` 托管 ExoPlayer 和 MediaSession
- `PlayerConnection` 使用异步 MediaController 连接服务
- 当前曲目、完整队列、队列索引、进度、缓冲、随机和循环状态通过 StateFlow 回传 UI
- Compose 队列支持点击跳转、上下调整、移除和确认清空
- 所有队列修改直接作用于 Media3 Timeline，因此继续复用既有持久化协议
- `PlaybackStateStore` 保存队列、索引、位置、随机和循环状态，包括 SAF 文档内容 URI
- 队列仅在 Timeline 变化时序列化；播放位置每 5 秒轻量保存
- 服务恢复后重建队列并停留在原位置，但保持暂停，避免意外自动播放

### 歌词链路

- 不重新接入旧版硬编码网络 API，也不依赖旧 `LrcView`
- 纯 Kotlin 解析标准 LRC 时间标签、同一行多时间戳、元数据与 `[offset:]`
- 活动行使用二分查找，避免每 500 ms 线性扫描整份歌词
- 用户从系统文件选择器导入 LRC，读取后复制到应用内部目录，不依赖长期外部 URI 权限
- UTF-8/BOM 解码失败时回退 GB18030，兼容常见中文历史歌词文件
- 文件 offset 与每首歌曲独立用户 offset 叠加，范围限制为 ±30 秒
- Compose 歌词列表随活动行滚动，高亮当前行；点击任意行按有效时间 Seek
- 删除歌词同时清除内部文件和该歌曲用户校准量

### 封面链路

- 优先读取专辑封面 URI
- `loadThumbnail` 或图片流解码失败后，使用 `MediaMetadataRetriever.embeddedPicture`
- 该回退同时支持 MediaStore 内容 URI 与 SAF 文档 URI
- 图片按请求尺寸采样，使用受限 LRU 内存缓存
- 读取失败时使用稳定 Material 3 渐变占位

## 4. UI 重写范围

已完成：

- Material 3 主题、动态配色、edge-to-edge 与响应式导航
- MediaStore 与 SAF 双来源歌曲、专辑、艺术家和文件夹浏览
- 收藏与最近播放智能分区、星标切换、清空历史和分区内搜索
- 桌面长按收藏/最近播放快捷入口，支持冷启动与热启动
- 联合搜索、精确集合筛选和集合内搜索
- 首次页支持“授予媒体权限”或“选择音乐目录”两种入口
- 设置页支持添加、移除、重新扫描目录，并展示权限状态和扫描 warning
- 权限、加载、空内容、错误状态和 Android 13+ 通知权限引导
- 真实封面、任意音频 URI 内嵌封面、缓存与错误占位
- 迷你播放器和全屏播放页
- 播放、暂停、切歌、Seek、随机和循环
- 队列位置展示、可视化编辑与持久恢复
- 本地同步 LRC 歌词、自动高亮、点击跳转、替换、删除和偏移调整
- 主题设置及迁移状态页

尚未达到完整产品能力：

- 高级智能列表、播放次数统计、最近添加和规则列表
- SAF 增量索引、磁盘缓存和提供程序变更监听
- 歌词文本编辑、双语/翻译、逐字歌词和联网 Provider
- 音频标签编辑器
- Jetpack Glance 桌面小组件
- Live2D 助手及模型切换
- Intro、购买、Bug Report 和远程功能
- 睡眠定时、均衡器和无缝播放
- 队列长按拖拽、批量选择和撤销

这些能力应继续在新架构上实现，不建议把旧 Fragment、ButterKnife、SlidingUpPanel 或失效在线接口重新接回活动模块。

## 5. 关键替换关系

| 旧方案 | 新方案 |
| --- | --- |
| Java/XML/Fragment 主界面 | Jetpack Compose Material 3 |
| Activity 内临时导航 | `MainUiState.destination` 统一路由 |
| 旧 DynamicShortcutManager / LauncherActivity | 平台 ShortcutManager + MainActivity Intent action |
| 旧数据库收藏/播放历史 | DataStore 稳定媒体 ID 集合与有序历史 |
| ButterKnife | Compose 状态与 Kotlin 属性 |
| SlidingUpPanel + MiniPlayerFragment | Compose MiniPlayer + 独立播放页面 |
| 旧 LrcView + 硬编码在线搜索 | 纯 Kotlin LRC parser + Compose 同步列表 + 本地导入 |
| 全盘扫描 / legacy storage | MediaStore + OpenDocumentTree 持久目录授权 |
| AppThemeHelper | Material 3 ColorScheme + DataStore |
| 生命周期 extensions / ViewModelProviders | Lifecycle 2.11 + `viewModelScope` / Compose collect |
| 自定义 MediaPlayer 服务控制 | Media3 ExoPlayer + MediaSessionService |
| 临时内存播放队列 | Media3 Timeline + Compose 编辑 + 持久化恢复 |
| Glide 3 封面链路 | ContentResolver 缩略图 + MediaMetadataRetriever + 采样 + LRU |
| SharedPreferences 主题状态 | DataStore |
| Fabric Crashlytics | 当前活动模块移除；需要时接入现代 Firebase Crashlytics |
| jcenter / 旧 JitPack 依赖 | Google Maven + Maven Central |

## 6. 仓库与安全清理

现代化分支删除了当前树中的签名材料、加密签名包、Firebase 配置和构建产物，并通过 `.gitignore` 阻止再次提交。删除当前树不会抹除历史；正式发布前仍需轮换旧签名/服务凭据。

用户导入歌词仅保存到应用内部存储，不上传网络。SAF 只保存系统授予的目录 URI 和只读权限；应用不申请写入权限，移除目录时主动释放持久 grant。收藏和最近播放仅保存媒体 ID，不复制音频或上传行为数据。

## 7. 验证门禁

CI 执行：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

自动测试覆盖搜索、Android 新旧路径、SAF 音频识别、稳定文档 ID、可读路径编码、跨来源去重、收藏切换、最近播放去重/排序/上限/不可用条目，以及 LRC 元数据、时间精度、多时间戳、非法时间、去重排序、偏移和活动行匹配。合并前仍建议人工验收：

1. Android 8、12、13、16/17 的媒体、通知、OpenDocumentTree 和系统文件选择器流程
2. 内部存储、SD 卡、USB OTG、云盘及第三方文档提供程序的持久授权与重启恢复
3. 10,000 首以上 MediaStore+SAF 混合库的扫描、聚合、搜索、收藏、历史、滚动和队列性能
4. 收藏来源暂时失效后恢复、历史上限、重复播放、清空和跨重启一致性
5. 不同 Launcher 上长按快捷入口，以及应用冷启动、后台和前台状态下的路由
6. 重叠授权目录、同名文件、未知 MIME、损坏音频和权限被系统撤销后的降级
7. UTF-8、带 BOM、GB18030、大文件、损坏和无时间标签 LRC
8. 歌词切歌竞态、活动行滚动、点击 Seek、正负 offset 与删除后重载
9. 队列跳转、连续重排、删除当前项、清空及服务重建后一致性
10. 后台播放、锁屏控制、蓝牙按键、进程回收、折叠屏、TalkBack 和高对比度

## 8. 后续优先级

### P0：发布可用性

- MediaSession、OpenDocumentTree、快捷入口、队列编辑、歌词导入和状态恢复的仪器化测试
- Baseline Profile、Macrobenchmark 与 10,000 首混合库性能基准
- SAF 权限撤销、不可读 URI、文件移动和播放错误恢复降级
- 超大队列、大歌词、收藏/历史和封面缓存压力测试
- OEM 前台服务、通知和 Launcher 快捷入口兼容验证

### P1：数据体验与旧功能迁移

- 播放次数、最近添加、最常播放、未播放和规则智能列表
- SAF 增量索引、磁盘缓存、目录面包屑和可选变更监听
- 音频标签编辑独立数据层
- 同目录同名歌词自动匹配、歌词编辑和双语歌词
- Glance 桌面小组件
- Live2D 独立 AndroidView 适配层

### P2：产品化

- 队列长按拖拽、批量操作和撤销
- 睡眠定时、均衡器入口和无缝播放
- 无障碍、键盘、遥控器、车机体验
- 截图测试、性能回归和签名发布流水线
