# ACG Player X 测试策略

## 1. 四层质量门禁

ACG Player X 2.0 当前使用四层验证：

1. **JVM 单元测试**：路径、合并去重、排序、歌单、M3U、LRC、播放状态、完成度、统计导出、Glance 文本状态和 benchmark 夹具选择。
2. **构建门禁**：Android Lint、JVM 测试、Debug APK、AndroidTest APK 与 benchmark APK。
3. **应用设备测试**：API 35 AOSP ATD 上运行 Compose、MediaSession、Glance、ContentResolver、DataStore 隐私清理和应用文件系统测试。
4. **性能与恢复门禁**：生成 Baseline/Startup Profile，运行冷启动、滚动 Macrobenchmark、10,000 首资料库和 `am force-stop` 恢复测试。

## 2. 本地命令

### 构建门禁

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  :benchmark:assembleBenchmarkBenchmark
```

### API 35 应用测试

```bash
./gradlew --no-daemon :app:pixel2Api35DebugAndroidTest
```

### Profile 生成

```bash
./gradlew --no-daemon :app:generateBaselineProfile
```

### 性能与恢复套件

```bash
./gradlew --no-daemon --info \
  :benchmark:pixel2Api35BenchmarkBenchmarkAndroidTest
```

托管设备固定为 Pixel 2、API 35、AOSP ATD、x86_64，并关闭系统动画。Linux 环境必须提供 KVM。

## 3. 应用仪器化覆盖

| 测试类 | 覆盖 |
| --- | --- |
| `MainActivityInstrumentedTest` | 音乐库/设置导航、暖启动快捷入口、Activity recreate |
| `PlaybackServiceInstrumentedTest` | MediaController 连接 MediaSession、初始暂停、播放命令和小组件快照 |
| `PlaybackWidgetStateStoreInstrumentedTest` | 小组件空状态、跨实例持久化、Receiver 与 provider metadata |
| `PlaybackStateStoreInstrumentedTest` | 队列/位置/随机/循环持久化，损坏 JSON 自动清理 |
| `PlaylistImportPreviewInstrumentedTest` | 待处理筛选、确认按钮和手工映射 |
| `PlaylistTransferRepositoryInstrumentedTest` | 文件 URI 导入、UTF-8 M3U8 导出、离线 ID 和再次导入 |
| `LyricsRepositoryInstrumentedTest` | UTF-8、GB18030、内部存储、偏移和删除清理 |
| `LibraryStatePrivacyInstrumentedTest` | 清除最近播放和统计后，收藏仍保持不变 |

## 4. Alpha19 统计测试

JVM 测试覆盖：

- 可用与暂不可用媒体共同参与汇总；
- 总播放、完成和收听时长稳定求和；
- JSON schema 版本、控制字符、反斜杠和引号转义；
- CSV 逗号、双引号和换行字段引用；
- 艺术家/专辑按累计收听时长聚合与稳定排序；
- 导出文件名扩展名；
- 并发加入的 `PlaybackDataExporter` 与统一 JSON 编码协议兼容。

设备测试在真实 DataStore 上写入收藏、最近播放和完成度统计，再执行隐私清理，确认：

- 最近播放为空；
- PlaybackStats 为空；
- 收藏 ID 保留；
- 清理过程不需要共享存储写权限。

自定义歌单、歌词和主题由独立 Store/文件管理，不在该清理路径中。

## 5. Glance 小组件测试边界

当前自动化门禁验证：

- Widget 状态文本的空白规范化和安全回退；
- SharedPreferences 轻量快照跨实例恢复；
- Manifest 中 Receiver 与 provider XML 配置；
- MediaSession Timeline/metadata 进入 PlaybackWidgetStateStore；
- Widget 代码、ActionCallback 与 AndroidTest APK 通过编译和 Lint。

API 35 AOSP ATD 不等于真实 Launcher。不同桌面网格、厂商 RemoteViews 限制、尺寸缩放、深色主题渲染和用户实际添加/移除小组件仍属于实机矩阵。

## 6. Benchmark 与恢复覆盖

| 测试 | 目标 |
| --- | --- |
| `BaselineProfileGenerator` | 捕获冷启动、资料库滚动和设置页路径，生成 Baseline/Startup Profile |
| `StartupBenchmark` | 对比无编译与 Baseline Profile 下的冷启动时序 |
| `SettingsScrollBenchmark` | 设置页滚动 FrameTiming |
| `LargeLibraryScrollBenchmark` | 10,000 首混合资料库双向滚动 FrameTiming |
| `PlaybackProcessRecoveryTest` | 建立队列、开启随机/循环、执行 `am force-stop`、重新启动并验证恢复 |

10,000 首夹具只在 benchmark 和 Profile 目标变体启用，不进入正常 Debug/Release 用户数据链路。

## 7. GitHub Actions 行为

- `push`：构建、Lint、JVM 测试并编译应用/测试/benchmark APK。
- `pull_request`：先运行构建门禁，再运行应用设备测试，最后运行 Profile、性能和强制停止恢复套件。
- `master` 与 `workflow_dispatch`：同样执行完整门禁。
- 应用设备报告保留 7 天；性能和 Profile 报告保留 14 天。

报告目录：

```text
modern-app/build/reports/androidTests/managedDevice/
modern-app/build/outputs/androidTest-results/managedDevice/
modern-app/src/main/generated/baselineProfiles/
benchmark/build/outputs/managed_device_android_test_additional_output/
benchmark/build/outputs/androidTest-results/managedDevice/
benchmark/build/reports/androidTests/managedDevice/
```

## 8. 播放状态耐久性

完整队列和常规位置更新在 `Dispatchers.IO` 上使用可确认落盘的 SharedPreferences 提交。生命周期回调保留非阻塞写入，避免主线程磁盘阻塞。

队列 Timeline 采用 300ms 去抖；快照在去抖结束后捕获，并在队列提交完成后补写最新位置和播放模式，避免快速切换随机/循环被旧快照覆盖。

小组件快照独立于完整播放恢复 JSON，仅保存渲染所需字段。Timeline、媒体切换、元数据和 `isPlaying` 变化采用去抖，避免快速事件风暴导致连续更新。

## 9. 已处理的测试陷阱

- 托管设备 ABI 固定为 `x86_64`。
- 快捷入口测试不替换 ActivityScenario 的原始启动 Intent。
- MediaController 使用主线程 Looper 创建和释放。
- Glance 颜色使用公开日夜 ColorProvider API。
- 性能夹具由 BuildConfig 和 Profile 构建类型双重限定。
- 测试失败时仍上传报告。
- Benchmark 模块通过显式 class filter 区分 Profile、Macrobenchmark 和恢复测试。
- 隐私测试使用唯一 ID，并在结束时移除测试收藏，避免跨方法污染。

## 10. 仍需真实设备验证

- OpenDocumentTree/OpenDocument 系统选择器和不同 DocumentsProvider
- SD 卡、USB、云盘、持久 URI 授权撤销和文件移动
- 系统重启、低内存杀进程和厂商后台限制
- 前台服务通知、锁屏、耳机、蓝牙和车机
- 不同 Launcher/OEM 的小组件添加、尺寸、深色模式和控制回调
- Android 8、12、13、16、17 多版本矩阵
- 截图、无障碍、PSS、GC 和目标硬件帧时间门槛

API 35 模拟设备结果不能替代这些发布前实机验收。
