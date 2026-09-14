# ACG Player X

ACG Player X 2.0 是一次面向现代 Android 的重写。当前活动应用已迁移到 **Jetpack Compose + Material 3 + Media3**；旧版 Java/XML/Fragment 源码仍保留为功能核对参考，但不参与默认构建。

当前开发版本：**2.0.0-alpha10**。

## 当前能力

### 音乐来源与浏览

- 使用 MediaStore 读取系统音乐库，并监听媒体变化后去抖刷新
- 使用 Storage Access Framework 持久授权一个或多个只读音乐目录
- 未授予完整媒体权限时，仍可只使用用户授权目录
- 递归扫描 MediaStore 未索引音频，并与系统媒体库合并、去重
- 按歌曲、收藏、最近播放、最近添加、最常播放、未播放、专辑、艺术家和文件夹浏览
- 搜索标题、艺术家、专辑、文件夹名称和可读路径
- 真实专辑封面、内嵌封面回退、采样解码和受限 LRU 缓存

### 自定义歌单

- Preferences DataStore 保存歌单名称、时间戳和有序媒体 ID
- 同一歌单可混合 MediaStore 曲目与 SAF 曲目
- 创建、重命名、删除、搜索、添加、移出、清空和播放歌单
- 单曲上移/下移，批量置顶、置底和移出
- 可搜索批量选择器，支持全选或取消当前搜索结果
- **长按拖动柄移动到任意目标位置**，支持列表边缘自动滚动
- 拖拽编辑使用本地草稿，用户点击保存时才进行一次 DataStore 原子写入
- 拖拽列表包含当前不可用项目，避免离线 SD 卡、USB 或云盘歌曲丢失相对位置
- 单曲、批量、清空、失效项清理和拖拽排序均提供 Snackbar 单步撤销
- M3U/M3U8 导入与 UTF-8 M3U8 导出
- 跨设备导入按经过校验的媒体 ID、内容 URI、文件名/目录和元数据分层重匹配

### 智能音乐库

- 收藏、最近播放、播放次数和最后播放时间本地持久化
- 最近添加、最常播放和未播放智能分区
- 智能分区支持搜索、收藏和按当前结果创建播放队列
- Android 7.1+ 动态快捷入口：收藏、最近播放、最近添加和最常播放

### 播放、队列与歌词

- Media3 ExoPlayer 后台播放和 MediaSession 系统控制
- 锁屏、通知栏、耳机和蓝牙媒体控制
- 播放、暂停、切歌、Seek、随机和循环
- 可视化播放队列：跳转、上下调整、移除和确认清空
- 队列、当前位置、随机和循环状态持久恢复；服务重建后保持暂停
- 本地 LRC 导入、纯 Kotlin 解析、逐行同步、点击跳转和每曲偏移校准
- UTF-8、UTF-8 BOM 与 GB18030 文本读取

### Compose 体验

- Material 3 动态配色、亮色、深色和跟随系统主题
- 手机 Bottom Navigation 与大屏 Navigation Rail
- 独立歌单入口、批量编辑器、拖拽排序器和 M3U 传输栏
- Android 13+ 播放通知权限引导
- 权限、加载、空内容、错误、Provider warning 和撤销状态

## 技术基线

| 项目 | 版本 |
| --- | --- |
| Android Gradle Plugin | 9.4.0 |
| Gradle | 9.6.0 |
| Kotlin / Compose Compiler plugin | 2.4.20 |
| compileSdk / targetSdk | 37 |
| minSdk | 23 |
| Compose BOM | 2026.08.00 |
| AndroidX Media3 | 1.11.0 |
| AndroidX Lifecycle | 2.11.0 |
| AndroidX DataStore | 1.2.1 |
| Java | 17 |

依赖版本集中在 `gradle/libs.versions.toml`，不再散落于模块脚本。

## 项目结构

```text
modern-app/                       当前活动的 Compose 应用
  src/main/kotlin/.../data/       MediaStore、SAF、智能库、歌单、M3U 与 DataStore
  src/main/kotlin/.../lyrics/     LRC 解析、内部存储与歌词状态
  src/main/kotlin/.../model/      领域模型与 MediaItem 映射
  src/main/kotlin/.../playback/   Media3 服务、控制器、队列与状态恢复
  src/main/kotlin/.../shortcuts/  Android 动态快捷入口
  src/main/kotlin/.../ui/         Compose 页面、批量编辑与拖拽排序
app/                              旧版应用源码，仅供迁移参考
appthemehelper/                   旧版主题辅助源码，仅供迁移参考
```

Gradle 中逻辑模块仍为 `:app`，实际目录通过 `settings.gradle.kts` 映射到 `modern-app/`。

## 构建与验证

要求 JDK 17 与 Android SDK 37：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

GitHub Actions 在 Push 和 Pull Request 上执行相同门禁。单元测试覆盖搜索、路径解析、SAF 稳定 ID、跨来源去重、智能列表、歌单编解码、批量顺序变换、拖拽目标索引与边界、M3U 重匹配，以及 LRC 时间轴。

## 歌单排序与撤销语义

批量置顶或置底始终保持所选歌曲在原歌单中的相对顺序。拖拽编辑器则展示完整媒体 ID 顺序，包括当前不可播放的离线项目。

拖拽期间仅修改 Compose 本地草稿；越过列表边缘时会自动滚动。用户点击“保存顺序”后，ViewModel 会验证新旧媒体 ID 集合完全一致，再通过一次 DataStore 更新提交最终顺序。取消对话框不会产生持久化写入。

以下操作会保存一份进程内顺序快照并提供 Snackbar 单步撤销：

- 单曲或批量移出
- 单曲上下交换
- 批量置顶或置底
- 长按拖拽排序
- 清空歌单
- 清理当前不可用项目

撤销只恢复歌单媒体 ID 顺序，不修改设备上的音频文件。开始下一次歌单修改、关闭对应歌单或 Snackbar 结束后，旧快照会释放。

## 权限与本地数据

- Android 13+ 系统媒体库：`READ_MEDIA_AUDIO`
- Android 12L 及以下：`READ_EXTERNAL_STORAGE`
- 用户指定目录：`OpenDocumentTree` + 持久只读 URI 权限
- M3U/M3U8：`OpenDocument` 与 `CreateDocument`
- Android 13+ 播放通知：`POST_NOTIFICATIONS`
- 后台播放：`FOREGROUND_SERVICE` 与 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`

应用不请求共享存储写权限、不启用明文网络，也不使用 `requestLegacyExternalStorage`。歌词、收藏、历史、播放统计和歌单均保存在本机；音频文件不会因歌单编辑而被复制、移动或删除。

## M3U 语义

M3U8 导出写入标准 `#EXTM3U`、`#PLAYLIST` 和 `#EXTINF`，并附带其他播放器可忽略的 `#ACGPLAYER-*` 注释。当前不可用项目使用 `acg-player://media/<id>` 占位，使其重新导入本应用时仍能恢复顺序。

导入支持 UTF-8、UTF-8 BOM 和 GB18030 回退。匹配顺序为：经过可移植提示校验的媒体 ID、经过校验的内容 URI、文件名与目录、标题/艺术家/时长。多候选条目不会静默猜测。单文件限制 4 MB，最多解析 20,000 条位置记录。

## 迁移状态

Compose 播放器核心、MediaStore + SAF 音乐库、歌单、批量编辑、任意位置拖拽排序、M3U/M3U8、封面、状态恢复、同步歌词、收藏、历史和基础智能列表均已进入活动模块。

仍待推进的重点：发布级仪器化测试、Baseline Profile 与 Macrobenchmark、M3U 导入预览和歧义手工映射、历史 MediaStore Playlist 导入、规则智能列表、歌词编辑、音频标签编辑、Glance 小组件与 Live2D 隔离适配。

- 详细技术说明：[`docs/MODERNIZATION.md`](docs/MODERNIZATION.md)
- 功能差距矩阵：[`docs/MIGRATION_MATRIX.md`](docs/MIGRATION_MATRIX.md)

## 安全说明

现代化分支删除了当前树中的旧签名材料、Firebase 配置和构建产物，但这些文件仍可能存在于 Git 历史。正式发布前应轮换旧签名和服务凭据；彻底清理需单独重写历史。

## License

沿用仓库现有 [`LICENSE.txt`](LICENSE.txt)。
