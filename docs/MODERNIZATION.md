# ACG Player X 现代化说明

## 1. 原项目问题

原工程主要实现停留在 2020 年，使用 Kotlin 1.3、AGP 4.0、Gradle 6.1、SDK 29、JDK 8、Groovy 脚本和 JCenter。界面由 Java Activity/Fragment、XML、ButterKnife、SlidingUpPanel、RealtimeBlurView 与自定义主题库组成。

结构性问题包括：Activity 职责过重、播放 UI 与 Fragment/第三方面板强耦合、旧存储模型、淘汰依赖、硬编码歌词接口、MediaStore Playlist 对 SAF 曲目不适用，以及仓库曾跟踪签名材料和发布产物。

## 2. 新构建基线

- Kotlin DSL 与 Version Catalog
- AGP 9.4.0 / Gradle 9.6.0 / JDK 17
- Kotlin 2.4.20 与 Compose Compiler plugin
- compileSdk / targetSdk 37，minSdk 23
- Compose BOM 2026.08.00
- Google Maven 与 Maven Central
- configuration cache、build cache 和并行构建

逻辑模块仍为 `:app`，实际映射到 `modern-app/`。旧 `app/` 与 `appthemehelper/` 不参与默认构建。

## 3. 当前架构

```text
MainActivity
  └── AcgPlayerApp
        ├── LibraryScreen
        ├── PlaylistsScreen
        │     ├── PlaylistTransferBar
        │     ├── PlaylistBatchEditorOverlay
        │     └── PlaylistDragReorderOverlay
        ├── NowPlayingScreen
        └── SettingsScreen

MainViewModel
  ├── MusicRepository / AuthorizedFolderRepository
  ├── LibraryStateRepository / LyricsRepository / SettingsRepository
  └── PlayerConnection ── MediaController

PlaylistViewModel
  ├── PlaylistRepository ── Preferences DataStore
  ├── PlaylistTransferRepository ── M3uPlaylistCodec
  ├── one-step undo snapshot
  └── PlayerConnection ── same MediaSession

PlaybackService
  ├── ExoPlayer
  ├── MediaSession
  └── PlaybackStateStore
```

`MainViewModel` 暴露不可变 `StateFlow<MainUiState>`。歌单使用独立 `PlaylistViewModel`，避免把歌单传输、批量编辑、拖拽草稿和撤销状态塞入主 ViewModel。

## 4. 音乐来源

- MediaStore 分版本读取 `RELATIVE_PATH` 或 `DATA`，保留 `DISPLAY_NAME`
- Android 10+ 最近添加优先使用 `DATE_ADDED`，缺失时回退 `DATE_MODIFIED`
- SAF 使用 OpenDocumentTree 持久只读授权
- `DocumentTreeMusicScanner` 使用 DocumentsContract BFS
- 单目录限制 20,000 项和 32 层深度，并检测已访问 document ID
- SAF 曲目由完整 URI 派生稳定负数 ID
- MediaStore 与 SAF 按 URI 和元数据指纹合并，冲突时优先 MediaStore
- Provider 错误、失效授权和扫描截断采用非阻断 warning

## 5. 歌单数据与编辑

`UserPlaylist` 保存 UUID、规范化名称、有序媒体 ID、创建时间和更新时间。MediaStore 正数 ID 与 SAF 稳定负数 ID 可以混排。当前不可访问的曲目只影响展示，不破坏持久顺序。

歌单 mutation 包括：创建、重命名、删除、单曲与批量添加、单曲或批量移出、相邻交换、批量置顶/置底、任意位置拖拽、清空和失效项清理。

### 批量编辑

批量选择器支持搜索、全选/取消当前结果和清空选择。置顶或置底按歌单持久顺序提取所选项，因此不受勾选先后影响。

### 长按拖拽排序

`PlaylistDragReorderOverlay` 展示完整媒体 ID 列表，而不是只展示当前可播放歌曲。不可用项目以占位行出现，用户仍可调整其位置，来源恢复后歌曲会出现在保存后的正确顺序。

拖拽通过右侧手柄启动：

1. 长按后触发触觉反馈并提升当前行。
2. 拖动中心越过可见目标行时，在本地草稿中移动媒体 ID。
3. 接近列表顶部或底部时调用 `LazyListState.scrollBy` 自动滚动。
4. 拖动过程不写 DataStore。
5. 用户点击“保存顺序”后，ViewModel 校验新旧列表长度和 ID 集合完全一致。
6. 通过一次 `replaceSongs` 原子提交最终顺序。
7. 提交前顺序进入现有撤销快照，Snackbar 可恢复完整列表。

取消对话框直接丢弃草稿。这样避免大型歌单在每次跨行时反复序列化和写盘。

### 撤销

除创建、重命名和删除整个歌单外，破坏性媒体 ID 修改会保存一次进程内完整顺序快照。开始新操作、离开歌单或 Snackbar 结束后，旧快照释放。撤销只恢复歌单结构，不触碰音频文件。

## 6. M3U/M3U8

导入通过 `OpenDocument`，单文件最大 4 MB，最多 20,000 个位置条目。支持 UTF-8、BOM 和 GB18030 回退，解析 `#EXTM3U`、`#PLAYLIST` 和 `#EXTINF`。

匹配顺序：

1. 经文件名和元数据提示校验的 ACG 媒体 ID
2. 经提示校验的精确内容 URI
3. 文件名和目录提示
4. 标题、艺术家和允许 3 秒误差的时长

歧义条目不会静默猜测。导出通过 `CreateDocument` 写 UTF-8 M3U8，并附带第三方播放器可忽略的 ACG 注释；不可用项目使用 `acg-player://media/<id>` 占位。

## 7. 播放、歌词与封面

- PlaybackService 托管 ExoPlayer 和 MediaSession
- PlayerConnection 异步连接 MediaController
- Timeline、索引、进度、缓冲、随机和循环通过 StateFlow 回传
- PlaybackStateStore 保存队列和位置；重建后保持暂停
- Compose 队列支持跳转、上下调整、移除和清空
- 纯 Kotlin LRC parser 支持多时间戳、元数据、offset、排序和去重
- 歌词导入后复制到内部存储，支持 UTF-8/BOM 与 GB18030
- 封面优先读取专辑 URI，失败后解析音频内嵌图片，并采样进入受限 LRU

## 8. UI 重写范围

已完成：

- Material 3、动态配色、edge-to-edge 和响应式导航
- MediaStore + SAF 音乐浏览、搜索和来源管理
- 收藏、最近播放、最近添加、最常播放和未播放
- 自定义歌单、批量编辑、任意位置长按拖拽、撤销和 M3U/M3U8
- 真实/内嵌封面、迷你播放器和全屏播放页
- Media3 队列编辑、恢复和系统控制
- 本地同步 LRC、点击跳转和偏移校准

尚未达到完整产品能力：

- 发布级仪器化测试、截图测试和性能基准
- M3U 导入预览、歧义手工映射和历史 MediaStore Playlist 导入
- SAF 增量索引、磁盘缓存和 Provider 变更监听
- 规则智能列表与播放完成度统计
- 歌词编辑、双语/翻译、逐字歌词和联网 Provider
- 音频标签编辑、Glance 小组件、Live2D、购买和发布流水线
- 睡眠定时、均衡器和无缝播放

## 9. 验证门禁

CI 执行：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

自动测试覆盖搜索、路径、MediaStore 时间、SAF、跨来源去重、智能列表、歌单编解码、批量顺序、拖拽最终索引与边界、M3U 跨设备重匹配和 LRC 时间轴。

合并前仍需人工或仪器化验收：

1. Android 8、12、13、16/17 的媒体、通知和 SAF 权限
2. 长歌单连续拖拽、边缘自动滚动、取消、保存和撤销
3. 离线 SD 卡/USB/云盘项目的占位与排序恢复
4. TalkBack、键盘、横屏、平板和折叠屏
5. MediaSession、蓝牙、音频焦点和服务重建
6. 10,000 首混合库、数千首歌单、超大队列和封面内存压力

## 10. 后续优先级

### P0：发布可用性

- MediaSession、OpenDocumentTree、歌单/M3U、快捷入口、歌词导入和进程回收仪器化测试
- Baseline Profile、Macrobenchmark、混合大库和大型歌单基准
- SAF 权限撤销、文件移动和播放错误恢复
- OEM 前台服务、通知和 Launcher 验证

### P1：数据体验

- M3U 导入预览、歧义手工映射和相对路径导出
- 历史 MediaStore Playlist 只读导入
- 类型安全、可恢复导航和歌单深链
- 规则智能列表、播放完成度和时间窗口统计
- SAF 增量索引、磁盘缓存和目录面包屑

### P2：旧特色与产品化

- 同目录歌词自动匹配、歌词编辑和双语歌词
- 音频标签编辑数据层
- Jetpack Glance 小组件
- Live2D AndroidView 隔离适配
- 睡眠定时、均衡器、无缝播放和发布流水线
