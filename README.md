# ACG Player X

ACG Player X 2.0 是面向现代 Android 的本地音乐播放器重写版。活动应用已迁移到 **Jetpack Compose + Material 3 + AndroidX Media3**；旧版 Java/XML/Fragment 源码仍保留为迁移参考，但不参与默认构建。

当前开发版本：**2.0.0-alpha12**。

## 已实现能力

### 本地音乐来源

- 使用 MediaStore 读取系统音乐库，并兼容 Android 新旧存储模型
- 使用 Storage Access Framework 持久授权一个或多个只读音乐目录
- 未授予完整媒体权限时，可仅使用用户选择的 SAF 目录
- 递归扫描 MediaStore 未索引音频，并合并、去重 MediaStore 与 SAF 曲目
- 监听 MediaStore 变化并去抖刷新；SAF 支持主动重新扫描
- 歌曲、专辑、艺术家、文件夹、收藏、最近播放、最近添加、最常播放和未播放视图
- 标题、艺术家、专辑、文件名与目录联合搜索

### 播放核心

- Media3 ExoPlayer + MediaSessionService 后台播放
- 系统媒体控件、锁屏、耳机、蓝牙和音频焦点
- 播放、暂停、Seek、上一首、下一首、随机与循环
- 队列可视化编辑：跳转、上下调整、移除和确认清空
- 持久化队列、当前曲目、位置、随机和循环状态
- 服务重建后恢复播放上下文，但保持暂停
- 真实专辑封面、音频内嵌封面回退、尺寸采样与受限 LRU 缓存

### 歌词

- 本地 LRC 导入和应用内部存储
- UTF-8、UTF-8 BOM 与 GB18030 解码
- 多时间戳、元数据、文件 offset、排序和去重
- 逐行同步、自动滚动、点击歌词 Seek
- 每首歌曲独立的 ±30 秒用户偏移校准

### 自定义歌单

- Preferences DataStore 保存 UUID、名称、有序媒体 ID 和时间戳
- 同一歌单可混合 MediaStore 与 SAF 曲目
- 新建、重命名、删除、搜索、添加、移出和清空
- 单曲上下调整、批量选择、批量置顶/置底/移出
- 长按拖拽到任意目标位置，支持长列表边缘自动滚动
- 暂不可用的 SD 卡、USB、云盘和 SAF 项目仍保留原始顺序
- 破坏性顺序修改提供 Material 3 Snackbar 单步撤销
- 按保存顺序播放或随机播放

### M3U / M3U8

- 通过系统 `OpenDocument` 导入，通过 `CreateDocument` 导出 UTF-8 M3U8
- 支持标准 `#EXTM3U`、`#PLAYLIST` 与 `#EXTINF`
- 导出附带可被第三方播放器忽略的 `#ACGPLAYER-*` 注释
- 当前不可用项目使用 `acg-player://media/<id>` 保留顺序
- 按媒体 ID、内容 URI、文件名/目录、标题/艺术家/时长分层匹配
- 对跨设备重复 MediaStore ID 与 `content://` URI 进行可移植元数据校验
- 文件限制 4 MB，最多解析 20,000 个位置条目
- 导入前预览全部条目，不再选中文件后立即创建歌单
- 自动匹配、离线保留、歧义、未匹配和重复条目分别标记
- 可排除自动结果、恢复自动结果、搜索完整音乐库并手工映射
- 最终创建保持原 M3U 条目顺序，并再次去重媒体 ID

### Compose 体验

- Material 3 动态配色、亮色、深色与跟随系统
- 手机 Bottom Navigation 与宽屏 Navigation Rail
- 权限、加载、错误、空内容、Provider warning 和导入结果状态
- Android 13+ 播放通知权限引导
- Android 7.1+ 收藏、最近播放、最近添加和最常播放动态快捷入口

### 工程质量

- Push 与 Pull Request 阻断执行 Android Lint、JVM 单元测试、Debug APK 和测试 APK 构建
- Pull Request、默认分支和手动运行使用 API 35 AOSP ATD 托管设备执行仪器化测试
- 托管设备固定为 Pixel 2 / API 35 / x86_64，并关闭系统动画
- 当前仪器化覆盖主界面导航、暖启动快捷入口、Activity 重建、MediaSession 连接、M3U 预览与手工映射、歌词文件往返和 M3U 文件往返
- 托管设备测试报告无论成功或失败都会作为 GitHub Actions artifact 保留 7 天

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
| AndroidX Test Runner | 1.7.0 |
| Java | 17 |

依赖版本集中在 `gradle/libs.versions.toml`。

## 项目结构

```text
modern-app/
  src/main/kotlin/.../data/       MediaStore、SAF、智能库、歌单、M3U 和 DataStore
  src/main/kotlin/.../lyrics/     LRC 解析、内部存储与歌词状态
  src/main/kotlin/.../model/      领域模型与 MediaItem 映射
  src/main/kotlin/.../playback/   Media3 服务、控制器、队列与状态恢复
  src/main/kotlin/.../shortcuts/  Android 动态快捷入口
  src/main/kotlin/.../ui/         Compose 页面、歌单编辑与导入预览
  src/test/                       纯 Kotlin / JVM 单元测试
  src/androidTest/                API 35 设备级与 Compose 仪器化测试
app/                              旧版应用源码，仅供迁移参考
appthemehelper/                   旧版主题辅助源码，仅供迁移参考
```

Gradle 中逻辑模块仍为 `:app`，实际目录映射到 `modern-app/`。

## 构建与验证

要求 JDK 17 与 Android SDK 37：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest
```

在支持 KVM 的 Linux 环境运行 API 35 托管设备测试：

```bash
./gradlew --no-daemon :app:pixel2Api35DebugAndroidTest
```

测试层级、覆盖矩阵、报告位置和剩余边界见 [`docs/TESTING.md`](docs/TESTING.md)。

## 权限与本地数据

- Android 13+ 系统音乐库：`READ_MEDIA_AUDIO`
- Android 12L 及以下系统音乐库：`READ_EXTERNAL_STORAGE`
- 用户指定目录：`OpenDocumentTree` + 持久只读 URI 权限
- M3U/M3U8 与歌词：系统单文件选择器
- Android 13+ 播放通知：`POST_NOTIFICATIONS`
- 后台播放：`FOREGROUND_SERVICE` 与 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`

应用不申请共享存储写权限，不启用明文网络，也不使用 `requestLegacyExternalStorage`。收藏、历史、播放统计、歌词索引和歌单保存在设备本地；应用不会上传音频或行为数据。

## 当前剩余重点

- 真实 DocumentsProvider/系统选择器、SAF 权限撤销、通知和强制进程回收仪器化测试
- Baseline Profile、Macrobenchmark、10,000 首混合库和大型歌单基准
- M3U 相对路径导出、旧 MediaStore Playlist 只读导入
- 规则智能列表和播放完成度统计
- SAF 增量索引、磁盘缓存和 Provider 变更监听
- 歌词编辑/双语/逐字、音频标签编辑、Glance 小组件与 Live2D 隔离适配
- 睡眠定时、均衡器、无缝播放和正式发布流水线

- 技术说明：[`docs/MODERNIZATION.md`](docs/MODERNIZATION.md)
- 迁移矩阵：[`docs/MIGRATION_MATRIX.md`](docs/MIGRATION_MATRIX.md)
- 测试策略：[`docs/TESTING.md`](docs/TESTING.md)

## 安全说明

现代化分支已删除当前树中的旧签名材料、Firebase 配置和发布产物，但 Git 历史中的旧凭据不会自动消失。正式发布前仍需轮换签名与服务凭据。

## License

沿用仓库现有 [`LICENSE.txt`](LICENSE.txt)。
