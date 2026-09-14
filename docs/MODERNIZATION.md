# ACG Player X 现代化说明

## 1. 目标与基线

原项目主要实现停留在 2020 年，采用 Java/XML/Fragment、ButterKnife、SlidingUpPanel、旧 MediaPlayer 控制、legacy storage、Fabric、JCenter 和旧 Gradle 工具链。现代化分支将活动应用替换为 Kotlin DSL、Version Catalog、Jetpack Compose、Material 3、Media3、DataStore 和 SAF。

当前基线：AGP 9.4、Gradle 9.6、Kotlin 2.4.20、JDK 17、compile/target SDK 37、minSdk 23。

逻辑模块仍为 `:app`，实际目录映射到 `modern-app/`；旧 `app/` 与 `appthemehelper/` 不参与默认构建。

## 2. 当前架构

```text
MainActivity
  ├── cold/warm shortcut action handling
  └── AcgPlayerApp
        ├── LibraryScreen
        ├── PlaylistsScreen
        │     ├── PlaylistTransferBar
        │     ├── PlaylistBatchEditorOverlay
        │     ├── PlaylistDragReorderOverlay
        │     └── PlaylistImportPreviewDialog
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
  ├── PlaylistRepository ── Preferences DataStore
  ├── PlaylistTransferRepository
  │     ├── M3uPlaylistCodec
  │     └── PlaylistImportPreview
  ├── one-step undo snapshot
  └── PlayerConnection ── same MediaSession

PlaybackService
  ├── ExoPlayer
  ├── MediaSession
  └── PlaybackStateStore
```

## 3. 音乐来源

- MediaStore 分版本读取 `RELATIVE_PATH` 或旧 `DATA`
- 保存 `DISPLAY_NAME`，最近添加优先使用 `DATE_ADDED`
- SAF 使用 `OpenDocumentTree` 持久只读授权
- DocumentsContract BFS 扫描，限制 20,000 项和 32 层
- SAF URI 通过 SHA-256 派生稳定负数媒体 ID
- MediaStore 与 SAF 按 URI 和元数据指纹合并，冲突时优先 MediaStore
- MediaStore 使用 ContentObserver 去抖刷新；SAF 使用进程内缓存与主动重扫

## 4. 播放与歌词

- PlaybackService 托管 ExoPlayer 和 MediaSession
- PlayerConnection 异步连接 MediaController
- 播放状态通过 `StateFlow<PlaybackUiState>` 驱动 Compose
- PlaybackStateStore 保存队列、索引、位置、随机和循环状态
- 服务重建后恢复上下文并保持暂停
- LRC 使用纯 Kotlin parser，导入后复制到应用内部目录
- 封面链路为专辑 URI → 图片流/缩略图 → 音频内嵌图片 → Material 3 占位

## 5. 自定义歌单

`UserPlaylist` 保存 UUID、规范化名称、有序媒体 ID 以及创建/更新时间。MediaStore 正数 ID 与 SAF 稳定负数 ID 可以混排。

歌单支持创建、重命名、删除、添加、移出、清空、搜索、顺序/随机播放、单曲交换、批量选择、批量置顶/置底/移出和任意位置长按拖拽。

拖拽期间只更新 Compose 本地草稿；保存时校验新旧媒体 ID 集合完全一致，并只执行一次 DataStore 写入。不可用媒体项目仍参与排序。破坏性媒体 ID 修改保存一次进程内快照，并通过 Snackbar 提供单步撤销。

## 6. M3U 导入与预览

导入链路为：

```text
OpenDocument
  ↓
读取与编码识别
  ↓
parseM3uPlaylist
  ↓
resolveM3uPlaylistDetailed
  ↓
PlaylistImportPreview
  ↓ 用户审查、排除、恢复或手工映射
finalizePlaylistImport
  ↓
一次性创建 UserPlaylist
```

支持 UTF-8、UTF-8 BOM 和 GB18030；单文件最大 4 MB，最多 20,000 个位置条目。标准字段包括 `#EXTM3U`、`#PLAYLIST` 和 `#EXTINF`，未知注释会被忽略。

匹配按可靠性递减：

1. 经过文件名/元数据校验的 ACG 媒体 ID
2. 经过同样校验的精确内容 URI
3. 文件名与目录提示
4. 标题、艺术家和允许 3 秒误差的时长

不同设备复用同一 MediaStore 数字 ID 或内容 URI 时，不会绕过可移植提示直接绑定。

每个原始条目保存索引、位置、元数据、自动结果、候选集合、状态和用户最终选择。用户可以搜索导入条目、仅看未处理项、排除或恢复自动结果、从候选中选择，或搜索完整音乐库进行手工映射。

同一媒体 ID 最终只保留第一次出现；最终歌单按原始 M3U 索引排序。取消预览不会创建歌单，也不会修改已有歌单。

## 7. M3U 导出

导出通过 `CreateDocument` 写入 UTF-8 M3U8。除标准 EXTINF 外，附带可忽略的 `#ACGPLAYER-MEDIA-ID`、文件名和目录注释。当前不可用项目使用 `acg-player://media/<id>` 保留顺序。

## 8. Alpha12 测试架构

### 静态与 JVM 门禁

Push 和 Pull Request 都执行：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest
```

这会阻断 Lint、单元测试、应用 APK 或测试 APK 的编译/打包回归。

### API 35 托管设备

Pull Request、默认分支和手动运行还会启动：

```text
Pixel 2
API 35
AOSP ATD
x86_64
系统动画关闭
```

设备测试通过 `:app:pixel2Api35DebugAndroidTest` 执行。Linux runner 会显式开放 `/dev/kvm`，失败或成功报告均上传为 7 天 artifact。

### 当前设备级覆盖

- Compose 壳层在音乐库与设置页之间导航
- 暖启动快捷入口进入收藏页
- Activity recreate 后保留当前目的地
- MediaController 连接 PlaybackService 的 MediaSession，并验证初始暂停状态
- M3U 预览待处理筛选与手工映射回调
- LyricsRepository 的 UTF-8 导入、内部存储读取、用户偏移和删除清理
- LyricsRepository 的 GB18030 回退解码
- PlaylistTransferRepository 的文件 URI 导入预览、UTF-8 M3U8 导出、离线项目与再导入

快捷入口测试通过独立动作分发入口验证 ViewModel 路由，不替换 ActivityScenario 的原始启动 Intent，避免生命周期跟踪失效。

详细测试命令和剩余边界见 `docs/TESTING.md`。

## 9. 仍需实机覆盖

- Android 8/12/13/16/17 的权限和前台服务差异
- 真实 DocumentsProvider、系统文件选择器与持久 URI 授权撤销
- 系统强制进程回收后的播放、导航和 DataStore 恢复
- 通知、锁屏、蓝牙、车机和 OEM 后台限制
- 10,000 首音乐库、数千首歌单、大队列和大歌词

## 10. 后续优先级

### P0

- Baseline Profile、Macrobenchmark、大型混合库/歌单/队列基准
- 真实 SAF Provider、权限撤销、文件移动和播放错误恢复测试
- 强制进程回收、通知和前台服务仪器化测试

### P1

- M3U 相对路径导出与旧 MediaStore Playlist 只读导入
- 类型安全、可恢复的统一导航和歌单深链
- 规则智能列表、播放完成度和时间窗口统计
- SAF 增量索引、磁盘缓存和目录面包屑

### P2

- 同目录歌词、歌词编辑、双语与逐字歌词
- 音频标签编辑、Glance 小组件和 Live2D 隔离层
- 睡眠定时、均衡器、无缝播放和发布流水线
