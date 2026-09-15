# ACG Player X 现代化说明

## 1. 目标与当前基线

原项目主要停留在 2020 年的 Java/XML/Fragment、ButterKnife、SlidingUpPanel、旧播放控制、legacy storage、Fabric、JCenter 和旧 Gradle 工具链。

当前活动实现为：

```text
Jetpack Compose + Material 3
Media3 ExoPlayer + MediaSessionService
MediaStore + SAF
DataStore + 本地文件存储
Baseline Profile + Macrobenchmark
```

工具链：AGP 9.4、Gradle 9.6、Kotlin 2.4.20、JDK 17、compile/target SDK 37、minSdk 23。当前版本为 `2.0.0-alpha15`。

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

PlaybackService
  ├── ExoPlayer
  ├── MediaSession
  └── PlaybackStateStore

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

## 5. 歌单、M3U 与歌词

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

LRC 使用纯 Kotlin parser，导入后复制到应用内部目录，支持 UTF-8/GB18030、文件 offset、每曲偏移、逐行同步和点击跳转。

## 6. 性能工程

独立 benchmark 模块目标为 release-like benchmark 变体。Profile 插件将生成规则合并到应用主源集，ProfileInstaller 为兼容设备提供安装支持。

性能夹具固定为 10,000 首：7,000 MediaStore 风格、3,000 SAF 风格。夹具只在 benchmark 和 Profile 目标变体启用。

门禁覆盖：

- 无编译与 Baseline Profile 冷启动；
- 设置页 FrameTiming；
- 10,000 首资料库 FrameTiming；
- 强制停止后的播放上下文恢复。

具体命令和限制见 `docs/PERFORMANCE.md`。

## 7. CI 门禁

```text
Lint + JVM tests + APK builds
  ↓
API 35 application instrumentation
  ↓
Baseline/Startup Profile generation
  ↓
Process recovery + Macrobenchmark smoke suite
```

成功或失败均上传设备和性能报告。普通分支 Push 只运行构建层，PR、默认分支和手动运行执行完整设备与性能链路。

## 8. 安全与数据边界

- 不申请共享存储写权限
- 不使用 requestLegacyExternalStorage
- 不启用明文网络
- 音频、收藏、历史、统计、歌词和歌单均留在本地
- 当前树已删除旧签名材料、Firebase 配置和发布产物

Git 历史中的旧凭据不会因删除当前文件而消失，正式发布前仍须轮换。

## 9. 后续优先级

### P0

- 真实 DocumentsProvider、授权撤销、文件移动和系统选择器矩阵
- 低内存杀进程、系统重启、通知和 OEM 后台限制
- 目标硬件冷启动、帧时、PSS、GC 和首帧门槛

### P1

- M3U 相对路径导出与旧 MediaStore Playlist 只读导入
- 规则智能列表和播放完成度
- SAF 增量索引、磁盘缓存和目录层级导航

### P2

- 歌词编辑、双语与逐字歌词
- 标签编辑、Glance 小组件和 Live2D 隔离层
- 睡眠定时、均衡器、无缝播放和正式发布流水线
