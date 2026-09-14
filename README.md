# ACG Player X

ACG Player X 2.0 是一次面向现代 Android 的重写。活动应用模块已经迁移到 **Jetpack Compose + Material 3 + Media3**，旧版 Java/XML/Fragment 实现保留为迁移参考，但不再参与默认构建。

当前开发版本：**2.0.0-alpha02**。

## 当前能力

- 使用 MediaStore 读取设备本地音乐
- 按歌曲、专辑、艺术家和文件夹浏览
- 跨标题、艺术家、专辑、文件夹名称及路径搜索
- 精确进入某张专辑、某位艺术家或某个文件夹，并可继续在集合内搜索
- 读取真实专辑封面，包含缩略图采样、内存缓存和错误占位
- 监听 MediaStore 变化，新增、删除或修改音乐后自动去抖刷新
- Media3 ExoPlayer 后台播放
- MediaSession 系统媒体控件、耳机与蓝牙控制
- 迷你播放器与全屏播放页
- 播放、暂停、切歌、进度拖动、随机与循环模式
- 持久化播放队列、当前曲目、进度、随机和循环状态
- 服务重建后恢复播放上下文，但不会未经用户操作自动播放
- Android 13+ 播放通知权限引导
- Material 3 动态配色、亮色、深色与跟随系统主题
- 手机底部导航与大屏 Navigation Rail 响应式布局
- DataStore 设置持久化

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
  src/main/kotlin/.../data/       MediaStore、路径解析与 DataStore
  src/main/kotlin/.../model/      领域模型与 MediaItem 映射
  src/main/kotlin/.../playback/   Media3 服务、控制器与状态恢复
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

GitHub Actions 会在推送和 Pull Request 时执行相同门禁。

## 权限

- Android 13 及以上：`READ_MEDIA_AUDIO`
- Android 12L 及以下：`READ_EXTERNAL_STORAGE`
- Android 13 及以上播放通知：`POST_NOTIFICATIONS`
- 后台播放：`FOREGROUND_SERVICE` 与 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`

应用不再请求写入共享存储，不再启用明文网络，也不再使用 `requestLegacyExternalStorage`。

## 播放恢复语义

播放器会保存队列结构、当前索引、播放位置、随机模式和循环模式。队列只在时间线发生变化时写入，位置采用轻量周期保存，避免持续序列化大型音乐库。服务或进程重新创建后会恢复上下文并保持暂停，防止应用在后台无意自动出声。

## 迁移状态

本地播放器核心闭环、文件夹浏览、真实封面、媒体库自动刷新和播放状态恢复已经迁入 Compose 模块。旧版 Live2D、歌词、联网搜索、标签编辑、桌面小组件、购买流程和部分高级播放能力仍待迁移。

- 详细技术说明：[`docs/MODERNIZATION.md`](docs/MODERNIZATION.md)
- 功能差距矩阵：[`docs/MIGRATION_MATRIX.md`](docs/MIGRATION_MATRIX.md)

## 安全说明

现代化分支删除了仓库中已跟踪的签名文件、加密签名包、Firebase 配置和构建产物。由于这些文件曾出现在 Git 历史中，正式发布前仍应轮换旧签名/服务凭据，并通过 GitHub Secrets 或本地未跟踪文件注入。

## License

沿用仓库现有 [`LICENSE.txt`](LICENSE.txt)。
