# ACG Player X 现代化说明

## 1. 原项目审计

原工程的主要实现停留在 2020 年，活动构建链路包含 Kotlin 1.3、AGP 4.0、Gradle 6.1、SDK 29、JDK 8、Groovy 脚本和 JCenter。界面层由 Java Activity/Fragment、XML、ButterKnife、SlidingUpPanel、RealtimeBlurView 与自定义主题辅助库组成。

主要结构性问题包括：

- 单个 Activity 同时承担导航、权限、Live2D、网络检查和页面切换
- 播放 UI 与 Fragment 和第三方滑动面板强耦合
- 旧歌词页依赖硬编码在线接口，LRC 核心解析不完整
- 歌单依赖 MediaStore Playlist、Loader 和多组 Dialog，难以覆盖 SAF 曲目
- 快捷入口依赖独立 Launcher Activity 和图标生成器
- legacy storage、Fabric、Kotlin Android Extensions、`lifecycle-extensions` 等淘汰组件
- 依赖版本散落，仓库曾跟踪签名文件、Firebase 配置和发布产物

## 2. 新构建基线

- Kotlin DSL 与 Version Catalog
- AGP 9.4.0 / Gradle 9.6.0 / JDK 17
- Kotlin 2.4.20 与 Compose Compiler plugin
- compileSdk / targetSdk 37，minSdk 23
- Compose BOM 2026.08.00
- Google Maven 与 Maven Central
- 非传递、非 final R 类
- Gradle build cache、configuration cache 与并行构建

逻辑模块仍为 `:app`，实际目录映射到 `modern-app/`。旧 `app/` 与 `appthemehelper/` 不参与默认构建，只用于核对历史行为。

## 3. 当前架构

```text
MainActivity
  ├── cold/warm shortcut intent handling
  └── AcgPlayerApp
        ├── LibraryScreen
        ├── PlaylistsScreen
        │     ├── M3U/M3U8 transfer bar
        │     ├── playlist overview / detail
        │     ├── searchable song picker
        │     └── PlaylistBatchEditorOverlay
        ├── NowPlayingScreen
        │     ├── cover
        │     ├── synchronized lyrics
        │     └── editable queue
        └── SettingsScreen

MainViewModel
  ├── MusicRepository
  ├── AuthorizedFolderRepository
  ├── LibraryStateRepository
  ├── LyricsRepository
  ├── SettingsRepository
  └── PlayerConnection ── MediaController

PlaylistViewModel
  ├── PlaylistRepository
  ├── PlaylistTransferRepository
  │     └── M3uPlaylistCodec
  ├── single-step undo snapshot
  └── PlayerConnection ── same MediaSession

PlaybackService
  ├── ExoPlayer
  ├── MediaSession
  └── PlaybackStateStore
```

### 状态与导航

`MainViewModel` 暴露 `StateFlow<MainUiState>`，负责音乐库、权限、授权目录、智能列表、主题、播放状态和歌词。主目的地由 `MainUiState.destination` 驱动，桌面快捷入口在 `MainActivity.onCreate()` 和 `onNewIntent()` 中转交 ViewModel。

歌单使用独立 `PlaylistViewModel`，避免把歌单编辑、撤销和文件传输状态塞进主 ViewModel。当前歌单入口由 Compose shell 的 `rememberSaveable` 控制，并拥有独立返回栈；后续可统一迁移到类型安全导航和可恢复 route。

### 音乐来源链路

- MediaStore 分版本读取 `RELATIVE_PATH` 或 `DATA`，同时保留 `DISPLAY_NAME`
- MediaStore 最近添加时间优先使用 `DATE_ADDED`，缺失时回退 `DATE_MODIFIED`
- SAF 使用 OpenDocumentTree 持久只读授权
- `DocumentTreeMusicScanner` 使用 DocumentsContract BFS，并保存文档显示名
- 单目录限制 20,000 项、32 层，并检测已访问目录 ID
- SAF 曲目通过完整 URI 的 SHA-256 生成稳定负数 ID
- MediaStore 与 SAF 按 URI 和元数据指纹合并，冲突时优先 MediaStore
- SAF 扫描使用进程内缓存；失效授权与 Provider 异常作为非阻断 warning

### 自定义歌单链路

```text
PlaylistsScreen / PlaylistBatchEditorOverlay / PlaylistTransferBar
  ↓ events
PlaylistViewModel
  ├── PlaylistRepository ── Preferences DataStore
  ├── one-step undo snapshot
  └── PlaylistTransferRepository ── OpenDocument/CreateDocument
                                      ↓
                                M3uPlaylistCodec
```

`UserPlaylist` 保存 UUID、规范化名称、有序媒体 ID 列表以及创建/更新时间。歌单不直接保存文件路径或复制音频。MediaStore 正数 ID 和 SAF 稳定负数 ID 可以混排。解析时根据当前完整音乐库恢复歌曲对象；无法解析的项目保留在 DataStore 中，因此可移动存储恢复后歌曲会重新出现。

歌单 mutation 包括：

- 创建、重命名和删除
- 单曲与批量添加
- 单曲或批量移出
- 单曲上下交换
- 批量置顶或置底
- 清空歌单
- 显式清理当前不可用项目

批量操作基于歌单的持久顺序执行。即使用户按不同顺序勾选歌曲，批量置顶或置底仍会保持这些歌曲在原歌单中的相对顺序，避免批处理意外打乱编排。

除创建、重命名和删除整个歌单外，破坏性媒体 ID 修改都会保存一次进程内快照，并通过 Material 3 Snackbar 提供单步撤销。开始新的歌单修改、离开对应歌单或 Snackbar 超时后，旧快照会释放。撤销只恢复媒体 ID 顺序，不修改音频文件。

名称会折叠连续空白、限制为 80 个字符，并进行忽略大小写的同名校验。持久格式转义换行、制表符、回车和反斜杠；解码器跳过损坏行而不阻塞其他歌单。

歌单播放仍通过同一个 `MediaSessionService`。`PlaylistViewModel` 只负责把解析后的有序歌曲转换为 Media3 队列，不创建第二个 ExoPlayer。

### M3U/M3U8 互操作链路

导入通过 `OpenDocument`，单文件最大 4 MB，最多解析 20,000 个位置条目。解码优先 UTF-8/UTF-8 BOM，失败时回退 GB18030。解析器支持标准 `#EXTM3U`、`#PLAYLIST` 和 `#EXTINF`，忽略未知注释。

匹配按可靠性递减：

1. 经过可移植文件名和元数据提示校验的 `#ACGPLAYER-MEDIA-ID`
2. 经过同样提示校验的精确内容 URI
3. 显示文件名与目录提示
4. 标题、艺术家和允许 3 秒误差的时长

多个候选无法唯一收敛时标记为歧义并跳过，不静默选错。重复媒体 ID 按首次出现顺序去重。导入名称冲突时使用递增后缀。

导出通过 `CreateDocument` 写入 UTF-8 M3U8。除标准 EXTINF 外，还写入可被第三方播放器安全忽略的 `#ACGPLAYER-MEDIA-ID`、文件名和目录注释。当前不可用项目使用 `acg-player://media/<id>` 占位，重新导入本应用时可恢复顺序。

### 智能音乐库链路

- 收藏、最近播放、播放次数和最后播放时间使用 Preferences DataStore
- 只有曲目真正进入播放状态时才记录
- 暂停继续同一媒体 ID 不重复计数
- 最近播放去重并移动到首位，最多 100 首
- 最常播放按次数、最后播放时间和标题稳定排序
- 未播放由当前可用库与播放统计求差集
- 当前来源不可用只影响展示，不破坏持久记录

### 播放、歌词和封面

- PlaybackService 托管 ExoPlayer 和 MediaSession
- PlayerConnection 异步连接 MediaController
- 当前曲目、完整队列、索引、进度、缓冲、随机和循环通过 StateFlow 回传
- Compose 队列支持跳转、上下调整、移除和清空
- PlaybackStateStore 保存队列、索引、位置和模式，服务恢复后保持暂停
- 纯 Kotlin LRC parser 支持多时间戳、元数据、offset、排序和去重
- OpenDocument 导入歌词后复制到应用内部存储，支持 UTF-8/BOM 与 GB18030
- 封面优先读取专辑 URI，失败后使用音频内嵌图片，按请求尺寸采样并进入受限 LRU

## 4. UI 重写范围

已完成：

- Material 3、动态配色、edge-to-edge 和响应式导航
- MediaStore + SAF 歌曲、专辑、艺术家和文件夹浏览
- 收藏、最近播放、最近添加、最常播放与未播放
- 自定义歌单概览、详情、搜索和歌曲选择器
- 歌单创建、重命名、删除、添加、移出、排序、清空和失效项清理
- 可搜索批量选择、批量置顶/置底、批量移出和 Snackbar 撤销
- 歌单顺序播放、随机播放和 M3U/M3U8 导入导出
- 四个 Android 动态快捷入口
- 真实/内嵌封面、迷你播放器和全屏播放页
- Media3 队列编辑与恢复
- 本地同步 LRC 歌词、点击跳转和偏移调整
- 权限、空内容、错误、加载、传输结果和 Provider warning 状态

尚未达到完整产品能力：

- 发布级仪器化测试和性能基准
- 歌单长按拖拽、任意目标位置移动、导入预览/手工映射及历史 MediaStore Playlist 导入
- SAF 增量索引、磁盘缓存和 Provider 变更监听
- 规则智能列表与播放完成度统计
- 歌词文本编辑、双语/翻译、逐字歌词和联网 Provider
- 音频标签编辑、Glance 小组件和 Live2D
- Intro、购买、Bug Report 与远程功能
- 睡眠定时、均衡器和无缝播放

## 5. 关键替换关系

| 旧方案 | 新方案 |
| --- | --- |
| Java/XML/Fragment 主界面 | Jetpack Compose Material 3 |
| ButterKnife | Compose 状态与 Kotlin 属性 |
| SlidingUpPanel + MiniPlayerFragment | Compose MiniPlayer + 独立播放页面 |
| 旧 LrcView + 硬编码在线搜索 | 纯 Kotlin LRC + Compose 同步列表 + 本地导入 |
| 全盘扫描 / legacy storage | MediaStore + OpenDocumentTree |
| MediaStore Playlist + Loader/Dialog | App-private PlaylistRepository + Compose 歌单/批量编辑 UI |
| 无可移植歌单协议 | M3U/M3U8 codec + SAF 单文件导入导出 + 分层重匹配 |
| 立即破坏式歌单编辑 | ViewModel 快照 + Material 3 Snackbar 单步撤销 |
| 旧 Shortcut Launcher Activity | ViewModel route + MainActivity 冷/热 Intent |
| 临时收藏/历史 | DataStore 收藏、最近播放与 PlaybackStats |
| AppThemeHelper | Material 3 ColorScheme + DataStore |
| 自定义 MediaPlayer 服务控制 | Media3 ExoPlayer + MediaSessionService |
| 临时队列 | Media3 Timeline + Compose 编辑 + 持久恢复 |
| Glide 3 封面链路 | ContentResolver + MediaMetadataRetriever + 采样 + LRU |
| Fabric / JCenter / Kotlin Android Extensions | 移除或使用现代官方组件 |

## 6. 本地数据与安全

现代化分支删除了当前树中的签名材料、加密签名包、Firebase 配置和构建产物，并通过 `.gitignore` 阻止再次提交。删除当前树不会抹除历史；正式发布前仍需轮换旧签名和服务凭据。

用户导入歌词仅保存到应用内部存储。SAF 目录只保存系统授予的 URI 和只读权限。收藏、历史、播放统计和歌单仅保存本地媒体标识及必要元数据，不上传行为数据，也不复制音频。

M3U 导入只读取用户显式选择的单个文档；导出只写入用户显式创建的目标文档。应用仍不申请共享存储写权限。

## 7. 验证门禁

CI 执行：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

自动测试覆盖：

- 搜索、路径解析、MediaStore 时间回退
- SAF 音频识别、稳定 ID、路径编码和跨来源去重
- 收藏、最近播放、播放统计和智能列表排序
- 歌单名称规范化、转义编解码、去重、单曲交换、批量移除和批量顺序变换
- M3U 标准字段、跨设备 ID/URI 冲突、文件名/元数据匹配、歧义保护和离线 ID
- LRC 时间线、偏移和活动行匹配

合并前仍建议人工验收：

1. Android 8、12、13、16/17 的媒体、通知和 SAF 权限流程
2. 不同文件选择器与 Provider 的 M3U 导入和 CreateDocument 行为
3. SD 卡、USB、云盘暂时离线后的歌单与离线 M3U ID 恢复
4. 大型歌单的搜索、全选当前结果、批量置顶/置底、批量移出和撤销
5. 重名、特殊字符、损坏行、重复条目、歧义文件名和超限文件
6. MediaSession、锁屏、蓝牙、音频焦点和服务重建
7. 10,000 首混合库、数千首歌单、超大队列和封面内存压力
8. 手机、平板、横屏、折叠屏、TalkBack 和高对比度

## 8. 后续优先级

### P0：发布可用性

- MediaSession、OpenDocumentTree、歌单/M3U、快捷入口、歌词导入和进程回收仪器化测试
- Baseline Profile、Macrobenchmark、混合大库和大型歌单基准
- SAF 权限撤销、文件移动和播放错误恢复
- OEM 前台服务、通知与 Launcher 验证

### P1：歌单与数据体验

- 长按拖拽和任意目标位置移动
- M3U 导入预览、歧义手工映射、可选相对路径导出
- 历史 MediaStore Playlist 只读导入
- 类型安全、可恢复的统一导航与歌单深链
- 规则智能列表、播放完成度和时间窗口统计
- SAF 增量索引、磁盘缓存与目录面包屑

### P2：旧特色与产品化

- 同目录歌词自动匹配、歌词编辑和双语歌词
- 音频标签编辑独立数据层
- Jetpack Glance 桌面小组件
- Live2D 独立 AndroidView 适配
- 睡眠定时、均衡器、无缝播放和发布流水线
