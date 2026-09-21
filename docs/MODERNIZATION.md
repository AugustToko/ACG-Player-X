# ACG Player X 现代化说明

## 1. 当前基线

原项目主要停留在 Java/XML/Fragment、ButterKnife、SlidingUpPanel、旧播放控制、legacy storage、Fabric、JCenter 和旧 Gradle 工具链。

当前活动实现：

```text
Jetpack Compose + Material 3
Media3 ExoPlayer + MediaSessionService
MediaStore + SAF
DataStore + 本地文件存储
Jetpack Glance AppWidget
Baseline Profile + Macrobenchmark
```

工具链为 AGP 9.4、Gradle 9.6、Kotlin 2.4.20、JDK 17、compile/target SDK 37、minSdk 23。当前版本为 `2.0.0-alpha19`。

逻辑模块仍为 `:app`，实际目录映射到 `modern-app/`；旧 `app/` 与 `appthemehelper/` 不参与默认构建。

## 2. 架构

```text
MainActivity
  └── AcgPlayerApp
        ├── LibraryScreen
        ├── PlaylistsScreen
        ├── NowPlayingScreen
        └── SettingsScreen
              └── ListeningStatisticsCard

MainViewModel
  ├── MusicRepository
  ├── AuthorizedFolderRepository
  ├── LibraryStateRepository
  ├── LyricsRepository
  ├── SettingsRepository
  └── PlayerConnection ── MediaController

StatisticsViewModel
  ├── PlaybackStatisticsSnapshot
  ├── JSON / CSV encoder
  └── LibraryStateRepository privacy reset

PlaylistViewModel
  ├── PlaylistRepository
  ├── PlaylistTransferRepository
  ├── M3U 预览/映射
  └── 单步撤销快照

PlaybackService
  ├── ExoPlayer
  ├── MediaSession
  ├── PlaybackStateStore
  ├── PlaybackProgressTracker ── LibraryStateRepository
  └── PlaybackWidgetStateStore ── PlaybackWidget.updateAll()

benchmark
  ├── BaselineProfileGenerator
  ├── StartupBenchmark
  ├── SettingsScrollBenchmark
  ├── LargeLibraryScrollBenchmark
  └── PlaybackProcessRecoveryTest
```

## 3. 音乐来源

- MediaStore 分版本读取 `RELATIVE_PATH` 或旧 `DATA`
- 保存 `DISPLAY_NAME`，最近添加优先 `DATE_ADDED`
- SAF 持久只读授权，DocumentsContract BFS 扫描
- 单目录限制 20,000 项和 32 层
- SAF URI 通过 SHA-256 派生稳定负数媒体 ID
- MediaStore 与 SAF 按 URI 和元数据指纹合并，冲突时优先 MediaStore
- MediaStore 使用 ContentObserver 去抖刷新；SAF 使用缓存和主动重扫

## 4. 播放、恢复与完成度

PlaybackService 托管 ExoPlayer 和 MediaSession。PlayerConnection 使用 MediaController 将状态暴露给 Compose。

PlaybackStateStore 保存完整队列、当前索引、位置、随机和循环模式。服务重建会恢复播放上下文、prepare 并保持暂停；Benchmark 设备测试执行真实 `am force-stop` 验证恢复。

完成度统计位于 PlaybackService，而不是 Activity：

```text
ExoPlayer 状态与位置
  → PlaybackProgressTracker
  → PlaybackProgressUpdate
  → LibraryStateRepository / DataStore
  → MainViewModel 智能列表与统计中心
```

位置增量与单调时钟交叉验证，明显大幅前跳视为 Seek，不计入累计收听。完成判定同时要求自然到达曲尾和足够有效收听时长，避免直接拖到结尾或 Repeat One 虚增。

PlaybackStats v2 保存：

- 播放次数和最后播放时间
- 完成次数和最后完成时间
- 累计收听时长
- 最后中途位置
- 已知时长

并兼容解码 v1 的三字段记录。

## 5. Alpha19：统计中心、导出与隐私

设置页新增“收听统计与隐私”卡片，直接复用 PlaybackStats v2，不维护第二套统计数据。

当前展示：

- 追踪项目数与当前可用项目数
- 总播放启动次数
- 总完整听完次数
- 累计收听时长
- 收听时长最高的歌曲
- 按收听时长聚合的艺术家和专辑

导出流程：

```text
PlaybackStats + 当前资料库元数据
  → PlaybackStatisticsSnapshot(schemaVersion = 1)
  → UTF-8 JSON / CSV
  → CreateDocument
```

导出保留当前不可用媒体 ID；可用曲目附带标题、艺术家、专辑和目录，便于本地分析。JSON 对控制字符和引号进行转义，CSV 遵循逗号、引号和换行字段引用规则。

隐私清理可独立选择：

- 最近播放顺序
- 播放次数、完成度、累计时长和恢复位置

清理不会删除收藏、自定义歌单、歌词、主题设置或媒体文件。正在播放的会话之后仍可重新产生统计。

并发加入的 `PlaybackDataExporter` API 被保留，但改为委托给同一无额外依赖的导出编码器，避免重复协议和未声明 serialization 依赖。

## 6. 智能音乐库与小组件

智能列表包括近 7 天、继续听、已听完和长音频；继续复用搜索、收藏、高亮和“按当前结果建立队列”的语义。

Glance 小组件通过 MediaController 连接现有 MediaSession，不维护第二套播放器。轻量快照只保存渲染所需媒体 ID、标题、艺术家、播放状态和前后项可用性。

## 7. 歌单、M3U 与歌词

自定义歌单支持 MediaStore 与 SAF 混排、批量编辑、任意位置拖拽、离线项目保序和 Snackbar 撤销。

M3U8 导出写入便携相对路径，并通过 `#ACGPLAYER-CONTENT-URI` 保留同设备精确 URI。旧 MediaStore Playlist 只提供一次性只读迁移，不回写系统表。

LRC 使用纯 Kotlin parser，导入后复制到应用内部目录，支持 UTF-8/GB18030、文件 offset、每曲偏移、逐行同步和点击跳转。

## 8. 性能与 CI

独立 benchmark 模块覆盖：

- 无编译与 Baseline Profile 冷启动
- 设置页 FrameTiming
- 10,000 首混合资料库 FrameTiming
- 强制停止后的播放上下文恢复

CI 链路：

```text
Lint + JVM tests + APK builds
  ↓
API 35 application instrumentation
  ↓
Baseline/Startup Profile generation
  ↓
Process recovery + Macrobenchmark smoke suite
```

Alpha19 新增 JVM 导出测试和 API 35 DataStore 隐私测试，确认清除收听数据不会删除收藏。

## 9. 安全与数据边界

- 不申请共享存储写权限
- 不使用 `requestLegacyExternalStorage`
- 不启用明文网络
- 音频、收藏、历史、统计、歌词、歌单和小组件快照均留在本地
- 统计导出只在用户选择的系统文件位置写入
- 当前树已删除旧签名材料、Firebase 配置和发布产物

Git 历史中的旧凭据不会因删除当前文件而消失，正式发布前仍须轮换。

## 10. 后续优先级

### P0

- 真实 DocumentsProvider、授权撤销、文件移动和系统选择器矩阵
- 低内存杀进程、系统重启、通知和 OEM 后台限制
- 目标硬件冷启动、帧时、PSS、GC 和首帧门槛

### P1

- 可配置的目录、时长、时间窗口、标签和完成度组合规则
- 统计重新导入与显式 schema migration registry
- SAF 增量索引、磁盘缓存和目录层级导航
- 小组件封面、进度和 Launcher/OEM 尺寸矩阵

### P2

- 歌词编辑、双语与逐字歌词
- 标签编辑和 Live2D 隔离层
- 睡眠定时、均衡器、无缝播放和正式发布流水线
