# ACG Player X 现代化说明

## 1. 目标与当前基线

原项目主要停留在 2020 年的 Java/XML/Fragment、ButterKnife、SlidingUpPanel、旧播放控制、legacy storage、Fabric、JCenter 和旧 Gradle 工具链。

当前活动实现为：

```text
Jetpack Compose + Material 3
Media3 ExoPlayer + MediaSessionService
MediaStore + SAF
DataStore + 本地文件存储
Jetpack Glance AppWidget
Baseline Profile + Macrobenchmark
```

工具链：AGP 9.4、Gradle 9.6、Kotlin 2.4.20、JDK 17、compile/target SDK 37、minSdk 23。当前版本为 `2.0.0-alpha18`。

逻辑模块仍为 `:app`，实际目录映射到 `modern-app/`；旧 `app/` 与 `appthemehelper/` 不参与默认构建。

## 2. 架构

```text
MainActivity
  └── AcgPlayerApp
        ├── LibraryScreen
        ├── PlaylistsScreen
        ├── NowPlayingScreen
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
  ├── M3U 预览/映射
  └── 单步撤销快照

PlaylistTransferBar
  └── LegacyMediaStorePlaylistRepository ── 只读兼容迁移

PlaybackService
  ├── ExoPlayer
  ├── MediaSession
  ├── PlaybackStateStore
  ├── PlaybackProgressTracker ── LibraryStateRepository
  └── PlaybackWidgetStateStore ── PlaybackWidget.updateAll()

PlaybackWidgetReceiver
  └── PlaybackWidget
        ├── TogglePlaybackWidgetAction ── MediaController
        ├── PreviousPlaybackWidgetAction ── MediaController
        └── NextPlaybackWidgetAction ── MediaController

benchmark
  ├── BaselineProfileGenerator
  ├── StartupBenchmark
  ├── SettingsScrollBenchmark
  ├── LargeLibraryScrollBenchmark
  └── PlaybackProcessRecoveryTest
```

## 3. 音乐来源

- MediaStore 分版本读取 RELATIVE_PATH 或旧 DATA
- 保存 DISPLAY_NAME，最近添加优先 DATE_ADDED
- SAF 持久只读授权，DocumentsContract BFS 扫描
- 单目录限制 20,000 项和 32 层
- SAF URI 通过 SHA-256 派生稳定负数媒体 ID
- MediaStore 与 SAF 按 URI 和元数据指纹合并，冲突时优先 MediaStore
- MediaStore 使用 ContentObserver 去抖刷新；SAF 使用缓存和主动重扫

## 4. 播放与恢复

PlaybackService 托管 ExoPlayer 和 MediaSession，PlayerConnection 通过异步 MediaController 将状态暴露给 Compose。

PlaybackStateStore 保存：

- 完整队列和媒体元数据；
- 当前索引和位置；
- 随机和循环模式。

完整队列与常规位置保存使用后台同步提交，以便调用完成时数据已经落盘。生命周期回调保留非阻塞写入，避免主线程磁盘 I/O。

Timeline 变化采用 300ms 去抖。快照在去抖结束后捕获，队列提交完成后再补写最新位置和模式，避免首次建队列时快速切换随机/循环被旧状态覆盖。

服务重建会恢复上下文、prepare，并保持暂停。Benchmark 设备测试进一步执行真实 `am force-stop` 和重新启动，验证当前曲目、随机和循环模式。

## 5. 播放完成度与智能音乐库

完成度统计位于 PlaybackService，而不是 Activity：

```text
ExoPlayer 状态与位置
  → PlaybackProgressTracker
  → PlaybackProgressUpdate
  → LibraryStateRepository / DataStore
  → MainViewModel 智能列表
```

服务每秒采样一次，但仅在以下条件写入：

- 未提交的有效收听达到 15 秒；
- 播放暂停；
- 切换媒体；
- 达到完成阈值。

位置增量会与 `SystemClock.elapsedRealtime()` 交叉验证。明显超过真实经过时间的大幅前跳视为 Seek，不计入累计收听时长。后退 Seek 会更新恢复位置，但不会生成负收听时间。

完成判定不是“位置接近结尾”单一条件：

- 30 秒以内音频要求达到 80%；
- 常规音频要求达到 90%；
- 长音频同时采用“距离结尾 5 分钟”保护；
- 还必须满足有效收听时长，防止直接拖到曲尾虚增完成次数。

PlaybackStats v2 保存播放次数、最后播放、完成次数、最后完成、累计收听、最后中途位置和已知时长，并兼容解码 v1 的三字段记录。

Compose 智能列表包括：

- 近 7 天；
- 继续听（有效收听至少 10 秒且进度 10%～89%）；
- 已听完；
- 长音频（20 分钟以上）。

这些列表继续复用原有搜索、收藏、高亮和“按当前结果建立队列”的语义。

## 6. Glance 桌面播放小组件

小组件不维护第二套播放器。所有控制通过 `MediaController` 连接现有 `PlaybackService` 和 `MediaSession`：

```text
Glance ActionCallback
  → MediaController
  → MediaSessionService
  → ExoPlayer
```

`PlaybackWidgetStateStore` 只保存渲染所需的轻量快照：

- 媒体 ID；
- 曲目标题；
- 艺术家；
- 播放/暂停状态；
- 上一首和下一首可用性。

PlaybackService 在 Timeline、媒体切换、元数据和 `isPlaying` 变化时，以 180ms 去抖写入快照并调用 `updateAll()`。小组件 ActionCallback 使用主线程 Looper 创建 MediaController，控制完成后更新快照并释放 Controller。

无队列状态不会尝试启动空播放，而是展示“打开播放器”。亮色和深色通过公开的 Glance 日夜 `ColorProvider(Color, Color)` 处理，不使用受限的资源型 ColorProvider。

## 7. 歌单、M3U 与歌词

自定义歌单保存 UUID、规范化名称、有序媒体 ID 和时间戳，支持 MediaStore 与 SAF 混排、批量编辑、拖拽、不在线项目保序和 Snackbar 撤销。

M3U 导入链路：

```text
OpenDocument
  → 编码识别
  → 解析
  → 分层自动匹配
  → 逐条预览/手工映射
  → 一次性创建歌单
```

M3U8 导出对当前可用歌曲写入便携相对路径，剥离 Android 存储根和 Windows 盘符，并中和 `..` 路径片段。同时使用 `#ACGPLAYER-CONTENT-URI` 保存同设备精确 URI，继续附带媒体 ID、文件名、目录和离线占位注释。第三方播放器可以忽略这些注释，ACG Player X 回导时则能先精确匹配，再回退到可移植元数据。

旧 MediaStore Playlist 自 API 31 起属于弃用兼容接口。本项目仅提供一次性只读迁移：读取名称、播放顺序和音频 ID，写入自己的 DataStore 歌单；不会创建、更新或删除系统 Playlist 记录。系统不再公开该表、权限不足或 OEM Provider 不兼容时，会给出可操作错误并保留 M3U 导入路径。

LRC 使用纯 Kotlin parser，导入后复制到应用内部目录，支持 UTF-8/GB18030、文件 offset、每曲偏移、逐行同步和点击跳转。

## 8. 性能工程

独立 benchmark 模块目标为 release-like benchmark 变体。Profile 插件将生成规则合并到应用主源集，ProfileInstaller 为兼容设备提供安装支持。

性能夹具固定为 10,000 首：7,000 MediaStore 风格、3,000 SAF 风格。夹具只在 benchmark 和 Profile 目标变体启用。

门禁覆盖：

- 无编译与 Baseline Profile 冷启动；
- 设置页 FrameTiming；
- 10,000 首资料库 FrameTiming；
- 强制停止后的播放上下文恢复。

具体命令和限制见 `docs/PERFORMANCE.md`。

## 9. CI 门禁

```text
Lint + JVM tests + APK builds
  ↓
API 35 application instrumentation
  ↓
Baseline/Startup Profile generation
  ↓
Process recovery + Macrobenchmark smoke suite
```

JVM 测试覆盖会话采样、前后 Seek、暂停/切歌提交、重复播放周期、完成阈值、v1/v2 统计兼容和智能列表排序。应用设备测试包含 Glance 状态持久化、Receiver provider metadata 和 MediaSession 到小组件快照的同步。成功或失败均上传设备和性能报告。

## 10. 安全与数据边界

- 不申请共享存储写权限
- 不使用 requestLegacyExternalStorage
- 不启用明文网络
- 音频、收藏、历史、完成度统计、歌词、歌单和小组件快照均留在本地
- 旧系统歌单迁移只读，不回写 MediaStore Playlist
- 小组件 ActionCallback 只连接应用自己的非导出 MediaSessionService
- 当前树已删除旧签名材料、Firebase 配置和发布产物

Git 历史中的旧凭据不会因删除当前文件而消失，正式发布前仍须轮换。

## 11. 后续优先级

### P0

- 真实 DocumentsProvider、授权撤销、文件移动和系统选择器矩阵
- 低内存杀进程、系统重启、通知和 OEM 后台限制
- 目标硬件冷启动、帧时、PSS、GC 和首帧门槛

### P1

- 可配置的目录、时长、时间窗口、标签和完成度组合规则
- 统计数据导出、清理和隐私重置入口
- SAF 增量索引、磁盘缓存和目录层级导航
- 小组件封面、播放进度和不同 Launcher/OEM 尺寸矩阵

### P2

- 歌词编辑、双语与逐字歌词
- 标签编辑和 Live2D 隔离层
- 睡眠定时、均衡器、无缝播放和正式发布流水线
