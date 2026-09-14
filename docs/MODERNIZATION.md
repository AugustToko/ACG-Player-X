# ACG Player X 现代化说明

## 1. 原项目审计结果

原工程最后一次提交停留在 2020 年，活动构建链路包含 Kotlin 1.3.72、Android Gradle Plugin 4.0、Gradle 6.1.1、compile/target SDK 29、JDK 8、Groovy 构建脚本和 `jcenter()`。界面层主要由 Java Activity/Fragment、XML、ButterKnife、SlidingUpPanel、RealtimeBlurView 与自定义主题辅助库组成。

主要风险包括：

- Fabric Crashlytics、Kotlin Android Extensions、`lifecycle-extensions` 等已淘汰组件
- 旧存储权限与 `requestLegacyExternalStorage`
- 单个 `MainActivity` 承担导航、权限、Live2D、网络检查、弹窗和页面切换
- 播放 UI 与 Fragment/第三方滑动面板强耦合
- 依赖版本散落且部分来自 JCenter/JitPack
- 仓库跟踪签名文件、Firebase 配置和 AAB 构建产物
- CI 依赖 JDK 8、旧 Actions 和签名解密脚本

## 2. 新构建基线

- Kotlin DSL 与 Version Catalog
- AGP 9.4.0 / Gradle 9.6.0 / JDK 17
- Kotlin 2.4.20 与 Compose Compiler Gradle plugin
- compileSdk / targetSdk 37，minSdk 23
- Compose BOM 2026.08.00
- 仅使用 Google Maven 与 Maven Central
- 非传递、非 final R 类
- Gradle build cache、configuration cache 与并行构建

逻辑模块名继续保持 `:app`，实际目录改为 `modern-app/`。旧 `app/` 与 `appthemehelper/` 不再被 `settings.gradle.kts` include，因此不会污染新构建，但仍可用于逐项核对旧行为。

## 3. 新架构

```text
MainActivity
  └── AcgPlayerApp (Compose responsive shell)
        ├── LibraryScreen
        ├── NowPlayingScreen
        └── SettingsScreen

MainViewModel
  ├── MusicRepository (MediaStore)
  ├── SettingsRepository (DataStore)
  └── PlayerConnection (MediaController)
                           └── PlaybackService
                                 └── MediaSession + ExoPlayer
```

### 状态管理

`MainViewModel` 暴露单一 `StateFlow<MainUiState>`。音乐库、搜索条件、当前页面、主题和播放状态都以不可变状态驱动 Compose，取消旧版 Activity 对 Fragment 视图的直接控制。

### 播放链路

- `PlaybackService` 托管 ExoPlayer 和 MediaSession
- `PlayerConnection` 使用异步 MediaController 连接服务
- 当前曲目、播放进度、缓冲、随机和循环状态通过 StateFlow 回传 UI
- 系统锁屏、通知栏、耳机与蓝牙控制统一走 MediaSession

### 数据链路

- `MusicRepository` 使用 MediaStore 查询音频，不再直接遍历共享存储
- `SettingsRepository` 使用 Preferences DataStore 存储主题模式
- 搜索过滤逻辑保持纯 Kotlin，并有单元测试覆盖

## 4. UI 重写范围

已完成：

- Material 3 主题与 Android 12+ 动态配色
- edge-to-edge Activity
- 手机底部导航与宽屏 Navigation Rail
- 本地歌曲列表、专辑聚合、艺术家聚合
- 标题/艺术家/专辑联合搜索
- 权限、加载、空内容和错误状态
- 迷你播放器
- 全屏正在播放页
- 播放、暂停、上一首、下一首、Seek、随机和循环
- 主题设置及迁移状态页

暂未达到旧版功能等价：

- Live2D 助手及模型切换
- LRC 歌词页与歌词组件
- 网易云等联网搜索/在线音乐
- 音频标签编辑器
- 文件夹浏览模式
- Android 桌面小组件
- Intro、购买、Bug Report 和旧 Firebase 远程功能
- 高级均衡器、睡眠定时、播放队列管理等扩展能力

这些能力应在新架构上逐项实现，不建议重新把旧 Fragment、ButterKnife 或 SlidingUpPanel 接回活动模块。

## 5. 依赖替换

| 旧方案 | 新方案 |
| --- | --- |
| Java/XML/Fragment 主界面 | Jetpack Compose Material 3 |
| ButterKnife | Compose 状态与 Kotlin 属性 |
| SlidingUpPanel + MiniPlayerFragment | Compose MiniPlayer + 独立播放页面 |
| AppThemeHelper | Material 3 ColorScheme + DataStore |
| 生命周期 extensions / ViewModelProviders | Lifecycle 2.11 + `viewModelScope` / Compose collect |
| 自定义 MediaPlayer 服务控制 | Media3 ExoPlayer + MediaSessionService |
| SharedPreferences 主题状态 | DataStore |
| Fabric Crashlytics | 当前活动模块移除；需要时接入现代 Firebase Crashlytics |
| jcenter / 旧 JitPack 依赖 | Google Maven + Maven Central |
| Kotlin Android Extensions | Compose / 标准 Kotlin |
| legacy external storage | MediaStore + 分版本读取权限 |

## 6. 仓库与安全清理

现代化分支删除：

- `demo.jks`
- `.github/secrets/*.gpg`
- 旧签名解密脚本
- `app/google-services.json`
- 已提交的 APK/AAB 输出与 metadata

`.gitignore` 现在阻止签名材料、Firebase 配置、构建产物和 IDE 文件再次提交。删除只影响当前树；历史中的凭据不会自动消失，因此正式发布前仍需轮换相关密钥。若必须彻底移除历史文件，应单独安排 Git 历史重写，并通知所有协作者重新同步仓库。

## 7. 验证门禁

CI 执行：

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug
```

建议合并前人工验收：

1. Android 8、12、13、16/17 的首次权限流程
2. 10,000 首以上音乐库的扫描与滚动性能
3. 中文、日文、英文元数据和未知元数据
4. 后台播放、锁屏控制、蓝牙按键与音频焦点
5. 进程回收后的 MediaSession 恢复行为
6. 横屏、折叠屏和平板宽度下的响应式布局
7. 动态配色、亮色、深色以及高对比度
8. 无音乐、权限拒绝、媒体库异常和损坏音频

## 8. 后续优先级

### P0：发布可用性

- 完善播放队列与持久恢复
- 真实专辑封面加载、缓存和错误占位
- Android 13+ 通知权限引导
- 媒体库变更监听与增量刷新
- 播放服务和 MediaController 测试
- Baseline Profile、Macrobenchmark 与大库性能基准

### P1：旧功能迁移

- Compose 歌词页和逐行同步
- SAF 文件夹入口与文件夹浏览
- 音频标签编辑的独立数据层
- Glance 小组件
- Live2D 以独立 AndroidView 适配层接入，避免污染主导航

### P2：产品化

- 播放队列编辑、收藏、历史与智能列表
- 睡眠定时、均衡器入口和无缝播放
- 无障碍、键盘/遥控器与车机体验
- 截图测试、性能回归和发布流水线
