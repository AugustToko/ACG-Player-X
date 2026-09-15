# ACG Player X

ACG Player X 2.0 是面向现代 Android 的本地音乐播放器重写版。活动应用已经迁移到 **Jetpack Compose + Material 3 + AndroidX Media3**；旧 Java/XML/Fragment 源码仍保留作迁移参考，但不参与默认构建。

当前开发版本：**2.0.0-alpha16**。

## 已实现能力

### 音乐来源与资料库

- MediaStore 系统音乐库，兼容 Android 新旧存储模型
- Storage Access Framework 多目录持久只读授权
- 未授予完整媒体权限时，可仅使用用户选择的 SAF 目录
- 递归扫描 MediaStore 未索引音频，合并并去重 MediaStore 与 SAF 曲目
- MediaStore 变化监听、去抖刷新、SAF 主动重扫和失效授权降级
- 歌曲、专辑、艺术家、文件夹、收藏、最近播放、最近添加、最常播放和未播放
- 标题、艺术家、专辑、显示文件名与目录联合搜索

### 播放核心

- Media3 ExoPlayer + MediaSessionService 后台播放
- 系统媒体控件、锁屏、耳机、蓝牙和音频焦点
- 播放、暂停、Seek、上一首、下一首、随机与循环
- 可视化队列：跳转、上下调整、移除与确认清空
- 持久化队列、当前项、位置、随机和循环状态
- 服务重建后恢复播放上下文并保持暂停
- 队列和位置的常规保存使用可确认落盘的后台写入
- API 35 性能设备会真实执行 `am force-stop`，验证队列、随机和循环模式恢复
- 专辑封面、音频内嵌图片回退、尺寸采样和受限 LRU 缓存

### 歌词

- 本地 LRC 导入并复制到应用内部存储
- UTF-8、UTF-8 BOM 与 GB18030 解码
- 多时间戳、元数据、文件 offset、排序与去重
- 逐行同步、自动滚动、点击歌词 Seek
- 每首歌曲独立的 ±30 秒偏移校准

### 自定义歌单与 M3U

- DataStore 保存 UUID、名称、有序媒体 ID 和时间戳
- 同一歌单混合 MediaStore 与 SAF 曲目
- 创建、重命名、删除、搜索、添加、移出、清空、顺序或随机播放
- 单曲调整、批量选择、批量置顶/置底/移出、长按拖拽与 Snackbar 撤销
- SD 卡、USB、云盘或 SAF 暂不可用时保留媒体 ID 和原始顺序
- OpenDocument 导入 M3U/M3U8，CreateDocument 导出 UTF-8 M3U8
- UTF-8/BOM 与 GB18030，4 MB 和 20,000 项安全限制
- 媒体 ID、内容 URI、文件名/目录、标题/艺术家/时长分层匹配
- 跨设备 MediaStore ID 与 `content://` URI 碰撞保护
- 导入预览、歧义候选、手工映射、排除/恢复自动结果和最终顺序去重
- 可用歌曲导出为便携相对路径，并通过 `#ACGPLAYER-CONTENT-URI` 保留同设备精确回导能力
- Android 存储根与 Windows 盘符会从导出路径中剥离，路径穿越片段会被中和
- 旧 MediaStore Playlist 支持一次性只读导入，保持播放顺序和媒体 ID，不回写或删除系统歌单

### Compose 体验

- Material 3 动态配色、亮色、深色与跟随系统
- 手机 Bottom Navigation 与宽屏 Navigation Rail
- 权限、加载、错误、空内容、Provider warning 和导入结果状态
- Android 13+ 播放通知权限引导
- Android 7.1+ 收藏、最近播放、最近添加和最常播放动态快捷入口

## 性能与质量门禁

- 独立 `:benchmark` 模块，目标为 release-like、不可调试的 `benchmark` 变体
- Baseline Profile 与 Startup Profile 生成并合入应用主源集
- 冷启动分别测量无编译和 Baseline Profile 两种模式
- 设置页滚动 FrameTiming Macrobenchmark
- 10,000 首混合资料库滚动 FrameTiming Macrobenchmark
- 10,000 首夹具由 7,000 个 MediaStore 风格和 3,000 个 SAF 风格条目组成，只在 benchmark/profile 变体启用
- API 35 AOSP ATD 托管设备执行 Compose、MediaSession、文件往返和进程恢复测试
- 性能、托管设备与 Profile 报告都会作为 GitHub Actions artifact 保留

详细说明见 [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) 和 [`docs/TESTING.md`](docs/TESTING.md)。

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
| AndroidX Benchmark / Baseline Profile | 1.5.0 |
| AndroidX Lifecycle | 2.11.0 |
| AndroidX DataStore | 1.2.1 |
| Java | 17 |

依赖版本集中在 `gradle/libs.versions.toml`。

## 项目结构

```text
modern-app/                       活动 Compose 应用
  src/main/kotlin/.../data/       MediaStore、SAF、智能库、歌单、M3U 与性能夹具
  src/main/kotlin/.../lyrics/     LRC 解析、存储与歌词状态
  src/main/kotlin/.../playback/   Media3、队列和恢复状态
  src/main/kotlin/.../ui/         Compose 页面和编辑器
  src/test/                       JVM 单元测试
  src/androidTest/                应用设备级测试
benchmark/                        Baseline Profile、Macrobenchmark 与强制停止恢复测试
app/                              旧版应用源码，仅供迁移参考
appthemehelper/                   旧版主题辅助源码，仅供迁移参考
```

Gradle 中活动模块仍为 `:app`，实际目录映射到 `modern-app/`。

## 构建与验证

要求 JDK 17 与 Android SDK 37：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  :benchmark:assembleBenchmarkBenchmark
```

API 35 应用仪器化测试：

```bash
./gradlew --no-daemon :app:pixel2Api35DebugAndroidTest
```

生成 Baseline / Startup Profile：

```bash
./gradlew --no-daemon :app:generateBaselineProfile
```

运行性能和强制停止恢复套件：

```bash
./gradlew --no-daemon \
  :benchmark:pixel2Api35BenchmarkBenchmarkAndroidTest
```

托管设备需要 Linux KVM。

## 权限与本地数据

- Android 13+ 系统音乐库：`READ_MEDIA_AUDIO`
- Android 12L 及以下：`READ_EXTERNAL_STORAGE`
- 用户目录：`OpenDocumentTree` + 持久只读 URI 权限
- M3U/M3U8 与歌词：系统单文件选择器
- 旧 MediaStore Playlist：仅在系统仍公开兼容表且已有音乐媒体权限时只读迁移
- Android 13+ 播放通知：`POST_NOTIFICATIONS`
- 后台播放：`FOREGROUND_SERVICE_MEDIA_PLAYBACK`

应用不申请共享存储写权限，不启用明文网络，也不使用 legacy external storage。收藏、历史、统计、歌词索引和歌单均保存在设备本地。

## 当前剩余重点

- 真实 DocumentsProvider、SD 卡、USB、云盘和持久授权撤销矩阵
- 低内存杀进程、系统重启、通知、锁屏、蓝牙、车机与 OEM 后台限制
- Android 多版本实机性能门槛和 PSS / GC 监控
- 规则智能列表和播放完成度统计
- SAF 增量索引、磁盘缓存与 Provider 变更监听
- 歌词编辑、双语/逐字歌词、音频标签编辑、Glance 小组件和 Live2D 隔离适配
- 睡眠定时、均衡器、无缝播放与正式签名发布流水线

- 技术说明：[`docs/MODERNIZATION.md`](docs/MODERNIZATION.md)
- 迁移矩阵：[`docs/MIGRATION_MATRIX.md`](docs/MIGRATION_MATRIX.md)
- 测试策略：[`docs/TESTING.md`](docs/TESTING.md)
- 性能策略：[`docs/PERFORMANCE.md`](docs/PERFORMANCE.md)

## 安全说明

现代化分支已删除当前树中的旧签名材料、Firebase 配置和发布产物，但 Git 历史中的旧凭据不会自动消失。正式发布前仍需轮换签名与服务凭据。

## License

沿用仓库现有 [`LICENSE.txt`](LICENSE.txt)。
