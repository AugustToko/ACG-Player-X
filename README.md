# ACG Player X

ACG Player X 2.0 是一次面向现代 Android 的重写。活动应用模块已经迁移到 **Jetpack Compose + Material 3 + Media3**，旧版 Java/XML/Fragment 实现保留为迁移参考，但不再参与默认构建。

## 当前能力

- 使用 MediaStore 读取设备本地音乐
- 按歌曲、专辑和艺术家浏览
- 跨歌曲标题、艺术家和专辑搜索
- Media3 ExoPlayer 后台播放
- MediaSession 系统媒体控件、耳机与蓝牙控制
- 迷你播放器与全屏播放页
- 播放、暂停、切歌、进度拖动、随机与循环模式
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
  src/main/kotlin/.../data/       MediaStore 与 DataStore
  src/main/kotlin/.../model/      领域模型
  src/main/kotlin/.../playback/   Media3 播放服务与控制器
  src/main/kotlin/.../ui/         Compose 页面与组件
app/                              旧版应用源码，仅供迁移参考
appthemehelper/                   旧版主题辅助源码，仅供迁移参考
```

Gradle 中逻辑模块仍为 `:app`，但通过 `settings.gradle.kts` 映射到 `modern-app/`，因此常用命令保持不变。

## 构建

要求 JDK 17 与 Android SDK 37：

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

GitHub Actions 会在推送和 Pull Request 时执行 lint、单元测试和 Debug APK 构建。

## 权限

- Android 13 及以上：`READ_MEDIA_AUDIO`
- Android 12L 及以下：`READ_EXTERNAL_STORAGE`
- 后台播放：`FOREGROUND_SERVICE` 与 `FOREGROUND_SERVICE_MEDIA_PLAYBACK`

应用不再请求写入共享存储，不再启用明文网络，也不再使用 `requestLegacyExternalStorage`。

## 迁移状态

本次重写优先恢复播放器的核心闭环。旧版 Live2D、歌词编辑/展示、联网搜索、标签编辑、桌面小组件、应用内购买和部分高级设置尚未迁入 Compose 模块。详细差距、替换关系和后续路线见 [`docs/MODERNIZATION.md`](docs/MODERNIZATION.md)。

## 安全说明

现代化分支删除了仓库中已跟踪的签名文件、加密签名包、Firebase 配置和构建产物。由于这些文件曾出现在 Git 历史中，正式发布前仍应轮换旧签名/服务凭据，并通过 GitHub Secrets 或本地未跟踪文件注入。

## License

沿用仓库现有 [`LICENSE.txt`](LICENSE.txt)。
