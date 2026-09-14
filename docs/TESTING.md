# ACG Player X 测试策略

## 1. 测试层级

ACG Player X 2.0 目前使用三层门禁：

1. **纯 Kotlin / JVM 单元测试**：验证路径、去重、排序、歌单编辑、M3U 匹配与 LRC 时间轴等确定性逻辑。
2. **Android 构建门禁**：执行 Lint、单元测试、应用 APK 和 AndroidTest APK 构建。
3. **API 35 托管设备测试**：在真实 Android framework、Compose、MediaSession、ContentResolver 和应用文件系统上执行设备级测试。

## 2. 本地命令

### 静态检查、JVM 测试和 APK

```bash
./gradlew --no-daemon \
  :app:lintDebug \
  :app:testDebugUnitTest \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest
```

### API 35 托管设备

```bash
./gradlew --no-daemon :app:pixel2Api35DebugAndroidTest
```

托管设备配置：

```text
设备模型：Pixel 2
API：35
镜像：aosp-atd
ABI：x86_64
动画：关闭
```

Linux 环境需要可访问 `/dev/kvm`。GitHub Actions 会在设备任务开始前调整 KVM 权限。

## 3. 当前仪器化覆盖

| 测试类 | 覆盖 |
| --- | --- |
| `MainActivityInstrumentedTest` | 音乐库/设置导航、暖启动快捷入口、Activity recreate 状态 |
| `PlaybackServiceInstrumentedTest` | MediaController 连接 MediaSession、初始暂停状态、播放命令可用性 |
| `PlaylistImportPreviewInstrumentedTest` | 待处理筛选、确认按钮、手工映射回调 |
| `PlaylistTransferRepositoryInstrumentedTest` | 文件 URI 导入预览、UTF-8 M3U8 导出、离线 ID、再次导入 |
| `LyricsRepositoryInstrumentedTest` | UTF-8 导入、内部存储读取、用户偏移、删除清理、GB18030 回退 |

当前合计 9 个设备级测试方法。

## 4. GitHub Actions 行为

- `push`：执行 Lint、JVM 测试、应用 APK 和 AndroidTest APK 构建。
- `pull_request`：先执行构建门禁，再运行 API 35 托管设备测试。
- `master` 与 `workflow_dispatch`：同样运行完整设备测试。
- 托管设备报告在成功或失败时都会上传，artifact 保留 7 天。

报告目录：

```text
modern-app/build/reports/androidTests/managedDevice/
modern-app/build/outputs/androidTest-results/managedDevice/
```

## 5. 已处理的测试陷阱

- 托管设备 ABI 显式固定为 `x86_64`，避免 AGP 10 默认 ABI 变化造成镜像不兼容。
- 快捷入口测试不替换 ActivityScenario 的原始启动 Intent；否则生命周期事件会因 Intent 不匹配而被忽略。
- MediaController 使用主线程 Looper 创建和释放，避免 Media3 线程契约错误。
- 设备失败报告始终上传，避免只看到 Gradle 的“存在失败测试”而没有具体断言。

## 6. 仍未覆盖

- 系统 `OpenDocumentTree` / `OpenDocument` 选择器交互
- 真实 DocumentsProvider、SD 卡、USB、云盘和持久 URI 授权撤销
- 系统强制停止、低内存杀进程和重启后的播放恢复
- 前台服务通知、锁屏、耳机、蓝牙、车机与 OEM 后台限制
- Android 8、12、13、16、17 多版本矩阵
- Compose 截图与无障碍测试
- Baseline Profile、Macrobenchmark、10,000 首混合库和大型歌单压力测试

这些项目应继续作为发布前门禁，而不能由当前 API 35 基础套件替代。
