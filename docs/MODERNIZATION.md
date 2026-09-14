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
  ├── MusicRepository
  │     ├── MediaStore query
  │     ├── folder/path normalization
  │     └── ContentObserver + debounce refresh
  ├── SettingsRepository (DataStore)
  └── PlayerConnection (MediaController)
                           └── PlaybackService
                                 ├── MediaSession + ExoPlayer
                                 └── PlaybackStateStore

Compose artwork
  └── ContentResolver thumbnail/stream loader
        ├── sampled decode
        ├── memory LRU cache
        └── generated placeholder
```

### 状态管理

`MainViewModel` 暴露单一 `StateFlow<MainUiState>`。音乐库、查询、精确集合筛选、当前页面、主题和播放状态都以不可变状态驱动 Compose，取消旧版 Activity 对 Fragment 视图的直接控制。

### 播放链路

- `PlaybackService` 托管 ExoPlayer 和 MediaSession
- `PlayerConnection` 使用异步 MediaController 连接服务
- 当前曲目、队列索引、播放进度、缓冲、随机和循环状态通过 StateFlow 回传 UI
- 系统锁屏、通知栏、耳机与蓝牙控制统一走 MediaSession
- `PlaybackStateStore` 保存队列、索引、位置、随机和循环状态
- 队列仅在时间线变化时序列化；播放位置每 5 秒轻量保存
- 服务恢复后重建队列并停留在原位置，但保持暂停，避免意外自动播放

### 数据链路

- `MusicRepository` 使用 MediaStore 查询音频，不再直接遍历共享存储
- Android 10+ 使用 `RELATIVE_PATH`，旧系统从 `DATA` 派生父目录
- 文件夹路径进入正式 `Song` 领域模型，不依赖 UI 临时计算
- MediaStore `ContentObserver` 监听增删改，并在 ViewModel 中进行 650 ms 去抖刷新
- `SettingsRepository` 使用 Preferences DataStore 存储主题模式
- 搜索和路径解析逻辑保持纯 Kotlin，并有单元测试覆盖

### 封面链路

- 优先读取专辑封面 URI
- 封面 URI失败时回退到歌曲内容 URI，尝试读取内嵌图片
- Android 10+ 使用 `ContentResolver.loadThumbnail`
- 旧系统使用按目标尺寸采样的 `BitmapFactory`
- 使用受限 LRU 内存缓存，避免列表滚动反复解码
- 读取失败时使用稳定的 Material 3 渐变占位

## 4. UI 重写范围

已完成：

- Material 3 主题与 Android 12+ 动态配色
- edge-to-edge Activity
- 手机底部导航与宽屏 Navigation Rail
- 本地歌曲列表、专辑聚合、艺术家聚合、文件夹聚合
- 标题、艺术家、专辑、文件夹名称及路径联合搜索
- 专辑、艺术家、文件夹精确筛选和集合内搜索
- 权限、加载、空内容和错误状态
- Android 13+ 通知权限非强制引导
- 真实封面、缓存与错误占位
- 迷你播放器
- 全屏正在播放页和队列位置展示
- 播放、暂停、上一首、下一首、Seek、随机和循环
- 播放队列与进度恢复
- 主题设置及迁移状态页

暂未达到旧版功能等价：

- Live2D 助手及模型切换
- LRC 歌词页、逐行同步和歌词编辑
- 网易云等联网搜索/在线音乐
- 音频标签编辑器
- Android 桌面小组件
- Intro、购买、Bug Report 和旧 Firebase 远程功能
- 播放队列可视化编辑、收藏、历史和智能列表
- 高级均衡器、睡眠定时、无缝播放等扩展能力

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
| 临时内存播放队列 | Media3 队列 + 持久化恢复层 |
| Glide 3 封面链路 | ContentResolver 缩略图 + 采样解码 + LRU 缓存 |
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

自动测试当前覆盖搜索字段和 Android 新旧路径解析。合并前仍建议人工验收：

1. Android 8、12、13、16/17 的媒体与通知权限流程
2. 10,000 首以上音乐库的扫描、聚合、搜索、滚动和队列持久化性能
3. 中文、日文、英文元数据和未知元数据
4. 真实封面、内嵌封面、无封面、损坏封面及大图片采样
5. 新增、删除、移动或修改音频后的自动刷新
6. 后台播放、锁屏控制、蓝牙按键与音频焦点
7. 进程回收、服务重建和设备重启后的队列与进度恢复
8. 横屏、折叠屏和平板宽度下的响应式布局
9. 动态配色、亮色、深色以及高对比度
10. 无音乐、权限拒绝、媒体库异常和损坏音频

## 8. 后续优先级

### P0：发布可用性

- 播放服务、MediaController 和状态恢复的仪器化测试
- Baseline Profile、Macrobenchmark 与大库性能基准
- 封面缓存命中率、内存压力和超大队列压力测试
- 播放错误、不可读 URI 和文件被移动后的恢复降级
- 前台服务与通知在各 OEM 系统上的兼容验证

### P1：旧功能迁移

- Compose 歌词页、LRC 解析、逐行同步和偏移调整
- SAF 文件夹入口，用于播放 MediaStore 未索引的用户授权目录
- 音频标签编辑的独立数据层
- Glance 桌面小组件
- Live2D 以独立 AndroidView 适配层接入，避免污染主导航

### P2：产品化

- 可视化播放队列编辑、收藏、历史与智能列表
- 睡眠定时、均衡器入口和无缝播放
- 无障碍、键盘、遥控器与车机体验
- 截图测试、性能回归和签名发布流水线
