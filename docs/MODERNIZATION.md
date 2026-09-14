# ACG Player X 现代化说明

## 1. 原项目审计结果

原工程最后一次提交停留在 2020 年，活动构建链路包含 Kotlin 1.3.72、Android Gradle Plugin 4.0、Gradle 6.1.1、compile/target SDK 29、JDK 8、Groovy 构建脚本和 `jcenter()`。界面层主要由 Java Activity/Fragment、XML、ButterKnife、SlidingUpPanel、RealtimeBlurView 与自定义主题辅助库组成。

主要风险包括：

- Fabric Crashlytics、Kotlin Android Extensions、`lifecycle-extensions` 等已淘汰组件
- 旧存储权限与 `requestLegacyExternalStorage`
- 单个 `MainActivity` 承担导航、权限、Live2D、网络检查、弹窗和页面切换
- 播放 UI 与 Fragment/第三方滑动面板强耦合
- 旧歌词页依赖硬编码在线接口，LRC 解析器主体实际上未完成
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
  └── AcgPlayerApp (Compose responsive shell)
        ├── LibraryScreen
        ├── NowPlayingScreen
        │     ├── cover pane
        │     ├── synchronized lyrics pane
        │     └── editable queue pane
        └── SettingsScreen

MainViewModel
  ├── MusicRepository
  │     ├── MediaStore query
  │     ├── folder/path normalization
  │     └── ContentObserver + debounce refresh
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

### 状态管理

`MainViewModel` 暴露单一 `StateFlow<MainUiState>`。音乐库、查询、精确集合筛选、主题、播放状态和当前歌词均以不可变状态驱动 Compose。曲目变化时取消上一首歌词任务并加载当前媒体 ID 对应缓存，异步结果写回前再次核对当前曲目，避免串歌。

### 播放与队列链路

- `PlaybackService` 托管 ExoPlayer 和 MediaSession
- `PlayerConnection` 使用异步 MediaController 连接服务
- 当前曲目、完整队列、队列索引、进度、缓冲、随机和循环状态通过 StateFlow 回传 UI
- Compose 队列支持点击跳转、上下调整、移除和确认清空
- 所有队列修改直接作用于 Media3 Timeline，因此继续复用既有持久化协议
- `PlaybackStateStore` 保存队列、索引、位置、随机和循环状态
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

### 音乐库与封面链路

- `MusicRepository` 使用 MediaStore 查询音频，不直接遍历共享存储
- Android 10+ 使用 `RELATIVE_PATH`，旧系统从 `DATA` 派生父目录
- MediaStore `ContentObserver` 监听增删改，并在 ViewModel 中进行 650 ms 去抖刷新
- 优先读取专辑封面 URI，失败时回退歌曲内容 URI 的内嵌图片
- Android 10+ 使用 `ContentResolver.loadThumbnail`，旧系统按目标尺寸采样解码
- 使用受限 LRU 内存缓存；读取失败时使用稳定 Material 3 渐变占位

## 4. UI 重写范围

已完成：

- Material 3 主题、动态配色、edge-to-edge 与响应式导航
- 本地歌曲、专辑、艺术家和文件夹浏览
- 联合搜索、精确集合筛选和集合内搜索
- 权限、加载、空内容、错误状态和 Android 13+ 通知权限引导
- 真实封面、缓存与错误占位
- 迷你播放器和全屏播放页
- 播放、暂停、切歌、Seek、随机和循环
- 队列位置展示、可视化编辑与持久恢复
- 本地同步 LRC 歌词、自动高亮、点击跳转、替换、删除和偏移调整
- 主题设置及迁移状态页

尚未达到完整产品能力：

- SAF 未索引目录和可恢复 URI 权限
- 歌词文本编辑、双语/翻译、逐字歌词和联网 Provider
- 音频标签编辑器
- Jetpack Glance 桌面小组件
- Live2D 助手及模型切换
- Intro、购买、Bug Report 和远程功能
- 收藏、历史、智能列表、睡眠定时、均衡器和无缝播放
- 队列长按拖拽、批量选择和撤销

这些能力应继续在新架构上实现，不建议把旧 Fragment、ButterKnife、SlidingUpPanel 或失效在线接口重新接回活动模块。

## 5. 关键替换关系

| 旧方案 | 新方案 |
| --- | --- |
| Java/XML/Fragment 主界面 | Jetpack Compose Material 3 |
| ButterKnife | Compose 状态与 Kotlin 属性 |
| SlidingUpPanel + MiniPlayerFragment | Compose MiniPlayer + 独立播放页面 |
| 旧 LrcView + 硬编码在线搜索 | 纯 Kotlin LRC parser + Compose 同步列表 + 本地导入 |
| AppThemeHelper | Material 3 ColorScheme + DataStore |
| 生命周期 extensions / ViewModelProviders | Lifecycle 2.11 + `viewModelScope` / Compose collect |
| 自定义 MediaPlayer 服务控制 | Media3 ExoPlayer + MediaSessionService |
| 临时内存播放队列 | Media3 Timeline + Compose 编辑 + 持久化恢复 |
| Glide 3 封面链路 | ContentResolver 缩略图 + 采样解码 + LRU 缓存 |
| SharedPreferences 主题状态 | DataStore |
| Fabric Crashlytics | 当前活动模块移除；需要时接入现代 Firebase Crashlytics |
| jcenter / 旧 JitPack 依赖 | Google Maven + Maven Central |
| legacy external storage | MediaStore + 分版本读取权限 |

## 6. 仓库与安全清理

现代化分支删除了当前树中的签名材料、加密签名包、Firebase 配置和构建产物，并通过 `.gitignore` 阻止再次提交。删除当前树不会抹除历史；正式发布前仍需轮换旧签名/服务凭据。用户导入歌词仅保存到应用内部存储，不上传网络。

## 7. 验证门禁

CI 执行：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

自动测试覆盖搜索、Android 新旧路径解析，以及 LRC 元数据、时间精度、多时间戳、非法时间、去重排序、偏移和活动行匹配。合并前仍建议人工验收：

1. Android 8、12、13、16/17 的媒体、通知与系统文件选择器流程
2. 10,000 首以上音乐库的扫描、聚合、搜索、滚动和队列持久化性能
3.  UTF-8、带 BOM、GB18030、大文件、损坏和无时间标签 LRC
4. 歌词切歌竞态、活动行滚动、点击 Seek、正负 offset 与删除后重载
5. 队列跳转、连续重排、删除当前项、清空及服务重建后一致性
6. 真实封面、内嵌封面、无封面、损坏封面及大图片采样
7. 后台播放、锁屏控制、蓝牙按键、耳机拔出和音频焦点
8. 进程回收、服务重建和设备重启后的队列与进度恢复
9. 横屏、折叠屏和平板宽度下的歌词和队列布局
10. 动态配色、亮色、深色、高对比度与 TalkBack

## 8. 后续优先级

### P0：发布可用性

- MediaSession、队列编辑、歌词导入和状态恢复的仪器化测试
- Baseline Profile、Macrobenchmark 与 10,000 首性能基准
- 超大队列、大歌词、封面缓存和内存压力测试
- 播放错误、不可读 URI 和文件移动后的恢复降级
- OEM 前台服务和通知兼容验证

### P1：数据入口与旧功能迁移

- SAF 文件夹入口和可恢复 URI 权限
- 音频标签编辑独立数据层
- 同目录同名歌词自动匹配、歌词编辑和双语歌词
- Glance 桌面小组件
- Live2D 独立 AndroidView 适配层

### P2：产品化

- 队列长按拖拽、批量操作和撤销
- 收藏、历史、智能列表、睡眠定时和均衡器入口
- 无障碍、键盘、遥控器、车机体验
- 截图测试、性能回归和签名发布流水线
