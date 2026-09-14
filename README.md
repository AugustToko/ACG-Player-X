# ACG Player X

ACG Player X 2.0 是一次面向现代 Android 的重写。活动应用已经迁移到 **Jetpack Compose + Material 3 + Media3**；旧版 Java/XML/Fragment 源码仍保留为迁移参考，但不参与默认构建。

当前开发版本：**2.0.0-alpha07**。

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

### 自定义歌单

- 使用独立 Preferences DataStore 保存歌单元数据和歌曲顺序
- 新建、重命名和删除歌单
- 混合添加 MediaStore 与 SAF 曲目
- 搜索歌单内歌曲
- 单曲添加、添加当前搜索结果、移出歌曲、上下调整和清空歌单
- 按歌单顺序播放或随机播放
- 统计可用歌曲、总时长和当前不可用项目
- SAF 目录暂时离线或权限失效时，只隐藏当前不可用歌曲，不破坏歌单结构
- 可显式清理当前不可用项目；不会删除设备上的音频文件

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
- 手机 Bottom Navigation 与大屏 Navigation Rail 响应式布局
- 独立歌单入口、歌单详情页和系统返回处理
- DataStore 设置、授权目录、歌单和本地音乐行为状态持久化

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
  src/main/kotlin/.../data/       MediaStore、SAF、智能库、歌单与 DataStore
  src/main/kotlin/.../lyrics/     LRC 解析、内部缓存与歌词状态
  src/main/kotlin/.../model/      领域模型与 MediaItem 映射
  src/main/kotlin/.../playback/   Media3 服务、控制器、队列与状态恢复
  src/main/kotlin/.../shortcuts/  Android 动态快捷入口
  src/main/kotlin/.../ui/         Compose 页面、歌单、封面和通用组件
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

GitHub Actions 会在推送和 Pull Request 时执行相同门禁。单元测试覆盖搜索、Android 新旧路径、MediaStore 时间回退、SAF 音频识别、稳定文档 ID、跨来源去重、收藏、播放统计、智能列表、歌单编解码/顺序/失效项，以及 LRC 时间轴和活动行匹配。

## 权限与本地数据

- Android 13 及以上系统媒体库：`READ_MEDIA_AUDIO`
- Android 12L 及以下系统媒体库：`READ_EXTERNAL_STORAGE`
- 用户指定目录：系统 `OpenDocumentTree` + 持久只读 URI 权限
- Android 13 及以上播放通知：`POST_NOTIFICATIONS`
- 后台播放：`FOREGROUND_SERVICE` 与 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`

系统媒体权限和 SAF 目录权限相互独立。用户可以不授予全盘媒体权限，只授权特定目录；应用会保存目录 URI，但不会上传路径或音频内容。移除目录时会释放对应持久权限。

收藏、历史、播放统计和自定义歌单只保存在设备本地。歌单保存名称、时间戳和媒体 ID 顺序，不复制音乐文件。应用不请求写入共享存储、不启用明文网络，也不使用 `requestLegacyExternalStorage`。

## 歌单语义

MediaStore 使用系统正数 ID，SAF 文档使用由 URI 派生的稳定负数 ID，因此二者可以出现在同一歌单中。歌单按照保存的媒体 ID 顺序解析当前可用歌曲。

当 SD 卡、USB、云盘或 SAF 目录暂时不可访问时，歌单保留原始项目，界面只显示当前可播放内容并报告不可用数量。恢复来源后，对应歌曲会重新出现。只有用户主动选择“清理失效项”时，才会移除这些媒体 ID。

同名歌单采用忽略大小写的唯一性校验；名称会折叠多余空白并限制为 80 个字符。歌单名称中的换行、制表符和反斜杠使用转义格式持久化。

## 智能列表语义

一次播放统计在歌曲真正进入播放状态且媒体 ID 与上一条已记录歌曲不同时产生。暂停后继续同一首歌不会重复计数；切换到其他歌曲后再次播放会新增一次。服务恢复时播放器保持暂停，因此不会生成虚假的最近播放或次数记录。

“最近添加”对 MediaStore 使用系统入库时间，对 SAF 使用文档提供程序暴露的最后修改时间。无法获得时间的歌曲仍会显示，但排在已知时间歌曲之后。

## SAF 扫描语义

每个授权目录最多检查 20,000 个目录项，最大递归深度为 32 层，避免异常文档提供程序造成无限递归或不可控扫描。SAF 扫描结果保存在进程内缓存；MediaStore 的普通变更不会重复遍历所有授权目录，用户可在设置页主动重新扫描。

当同一首歌同时存在于 MediaStore 和授权目录中时，应用优先保留 MediaStore 条目；重叠授权目录中的相同文档 URI 也会去重。失效授权、Provider 字段异常和扫描截断会显示为非阻断提示。

## 迁移状态

本地播放器核心闭环、MediaStore 与 SAF 双来源音乐库、自定义歌单、真实及内嵌封面、媒体库自动刷新、播放状态恢复、同步 LRC 歌词、可视化队列、收藏、最近播放和基础智能列表已经迁入 Compose 模块。

仍待迁移的重点包括 M3U 导入导出、规则智能列表、歌词编辑、联网音乐 Provider、音频标签编辑、Glance 小组件、Live2D、购买流程和发布级性能/仪器化测试。

- 详细技术说明：[`docs/MODERNIZATION.md`](docs/MODERNIZATION.md)
- 功能差距矩阵：[`docs/MIGRATION_MATRIX.md`](docs/MIGRATION_MATRIX.md)

## 安全说明

现代化分支删除了仓库当前树中已跟踪的签名文件、加密签名包、Firebase 配置和构建产物。由于这些文件曾出现在 Git 历史中，正式发布前仍应轮换旧签名/服务凭据，并通过 GitHub Secrets 或本地未跟踪文件注入。

## License

沿用仓库现有 [`LICENSE.txt`](LICENSE.txt)。
