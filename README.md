# ACG Player X

ACG Player X 2.0 是一次面向现代 Android 的重写。活动应用模块已经迁移到 **Jetpack Compose + Material 3 + Media3**，旧版 Java/XML/Fragment 实现保留为迁移参考，但不再参与默认构建。

当前开发版本：**2.0.0-alpha06**。

## 当前能力

### 音乐来源与浏览

- 使用 MediaStore 读取设备本地音乐
- 使用 Android Storage Access Framework 持久授权一个或多个音乐目录
- 未授予全盘媒体权限时，仍可只浏览和播放用户授权目录
- 递归读取 MediaStore 未索引的音频，并与系统媒体库合并、去重
- 按歌曲、收藏、最近播放、最近添加、最常播放、未播放、专辑、艺术家和文件夹浏览
- 跨标题、艺术家、专辑、文件夹名称及可读目录路径搜索
- 精确进入某张专辑、某位艺术家或某个文件夹，并继续在集合内搜索
- 监听 MediaStore 变化，新增、删除或修改音乐后自动去抖刷新

### 智能音乐库

- 收藏与最近播放使用 Preferences DataStore 持久化
- 播放次数与最后播放时间按媒体 ID 保存，同时支持 MediaStore 与 SAF 曲目
- 最近添加：MediaStore 优先使用 `DATE_ADDED`，缺失时回退 `DATE_MODIFIED`；SAF 使用文档最后修改时间
- 最常播放：按累计次数降序，次数相同时最近播放优先
- 未播放：仅显示当前可用音乐库中尚未产生播放记录的歌曲
- 智能列表支持搜索、收藏和按当前结果创建播放队列
- 当前来源暂时不可用时，不会破坏收藏、最近播放或播放统计
- Android 7.1+ 桌面长按快捷入口可直接打开收藏、最近播放、最近添加和最常播放

### 播放、封面与歌词

- Media3 ExoPlayer 后台播放与 MediaSession 系统控制
- 迷你播放器与全屏播放页
- 播放、暂停、切歌、进度拖动、随机与循环模式
- 可视化播放队列：跳转、上下调整、移除和确认清空
- 持久化播放队列、当前曲目、进度、随机和循环状态
- 服务重建后恢复播放上下文，但不会未经用户操作自动播放
- 读取真实专辑封面；任意文档 URI 支持音频内嵌封面回退、采样和 LRU 缓存
- 本地 LRC 歌词导入、纯 Kotlin 解析、逐行同步和点击跳转
- LRC 文件偏移与用户校准叠加，支持每首歌曲独立保存 ±30 秒校准量
- UTF-8/BOM 与 GB18030 歌词读取，导入后复制到应用内部缓存

### Compose 体验

- Android 13+ 播放通知权限引导
- Material 3 动态配色、亮色、深色与跟随系统主题
- 手机底部导航与大屏 Navigation Rail 响应式布局
- DataStore 设置、授权目录和本地音乐行为状态持久化

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

依赖版本集中在 `gradle/libs.versions.toml`，不再散落于各模块脚本。

## 项目结构

```text
modern-app/                       当前活动的 Compose 应用
  src/main/kotlin/.../data/       MediaStore、SAF、智能库状态与 DataStore
  src/main/kotlin/.../lyrics/     LRC 解析、内部缓存与歌词状态
  src/main/kotlin/.../model/      领域模型与 MediaItem 映射
  src/main/kotlin/.../playback/   Media3 服务、控制器、队列与状态恢复
  src/main/kotlin/.../shortcuts/  Android 动态快捷入口
  src/main/kotlin/.../ui/         Compose 页面、封面和通用组件
app/                              旧版应用源码，仅供迁移参考
appthemehelper/                   旧版主题辅助源码，仅供迁移参考
```

Gradle 中逻辑模块仍为 `:app`，但通过 `settings.gradle.kts` 映射到 `modern-app/`，因此常用命令保持不变。

## 构建与验证

要求 JDK 17 与 Android SDK 37：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

GitHub Actions 会在推送和 Pull Request 时执行相同门禁。单元测试覆盖搜索、Android 新旧路径、MediaStore 时间回退、SAF 音频识别、稳定文档 ID、目录路径编码、跨来源去重、收藏、最近播放、播放统计、智能列表排序，以及 LRC 时间轴和活动行匹配。

## 权限与本地数据

- Android 13 及以上系统媒体库：`READ_MEDIA_AUDIO`
- Android 12L 及以下系统媒体库：`READ_EXTERNAL_STORAGE`
- 用户指定目录：系统 `OpenDocumentTree` + 持久只读 URI 权限
- Android 13 及以上播放通知：`POST_NOTIFICATIONS`
- 后台播放：`FOREGROUND_SERVICE` 与 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`

系统媒体权限和 SAF 目录权限相互独立。用户可以不授予全盘媒体权限，只授权特定目录；应用会保存目录 URI，但不会上传路径或音频内容。移除目录时会释放对应持久权限。

收藏、最近播放、播放次数和最后播放时间只保存在设备上的 DataStore 中。应用不请求写入共享存储，不启用明文网络，也不使用 `requestLegacyExternalStorage`。用户选择的 LRC 文件只在导入时读取，之后复制到应用内部 `files/lyrics` 目录。

## 智能列表语义

一次播放统计在歌曲真正进入播放状态且媒体 ID 与上一条已记录歌曲不同时产生。暂停后继续同一首歌不会重复计数；切换到其他歌曲后再次播放会新增一次。服务恢复时播放器保持暂停，因此不会生成虚假的最近播放或次数记录。

“最近添加”对 MediaStore 使用系统入库时间，对 SAF 使用文档提供程序暴露的最后修改时间。无法获得时间的歌曲仍会显示，但排在已知时间歌曲之后。

## SAF 扫描语义

每个授权目录最多检查 20,000 个目录项，最大递归深度为 32 层，避免异常文档提供程序造成无限递归或不可控扫描。SAF 扫描结果保存在进程内缓存；MediaStore 的普通变更不会重复遍历所有授权目录，用户可在设置页主动“重新扫描”。

当同一首歌同时存在于 MediaStore 和授权目录中时，应用优先保留 MediaStore 条目；重叠授权目录中的完全相同文档 URI 也会去重。失效授权、提供程序字段异常和扫描截断会显示在设置页，而不会阻塞其他可用音乐来源。

## 迁移状态

本地播放器核心闭环、MediaStore 与 SAF 双来源音乐库、真实及内嵌封面、媒体库自动刷新、播放状态恢复、同步 LRC 歌词、可视化队列、收藏、最近播放和基础智能列表已经迁入 Compose 模块。

仍待迁移的重点包括规则智能列表、歌词编辑、联网音乐 Provider、音频标签编辑、Glance 小组件、Live2D、购买流程和发布级性能/仪器化测试。

- 详细技术说明：[`docs/MODERNIZATION.md`](docs/MODERNIZATION.md)
- 功能差距矩阵：[`docs/MIGRATION_MATRIX.md`](docs/MIGRATION_MATRIX.md)

## 安全说明

现代化分支删除了仓库中已跟踪的签名文件、加密签名包、Firebase 配置和构建产物。由于这些文件曾出现在 Git 历史中，正式发布前仍应轮换旧签名/服务凭据，并通过 GitHub Secrets 或本地未跟踪文件注入。

## License

沿用仓库现有 [`LICENSE.txt`](LICENSE.txt)。
