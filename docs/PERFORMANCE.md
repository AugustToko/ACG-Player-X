# ACG Player X 性能工程

## 1. 目标

性能门禁用于回答四个问题：

1. 冷启动是否因代码变化明显退化；
2. Baseline Profile 是否真实参与目标变体；
3. Compose 在大资料库和长页面中是否出现明显掉帧；
4. 进程被强制停止后，播放器上下文能否可靠恢复。

## 2. Benchmark 模块

独立 `:benchmark` 模块使用 `com.android.test`、Macrobenchmark 和 Baseline Profile 插件，目标为应用的 `benchmark` 构建类型。该变体继承 release 优化、使用 Debug 签名、不可调试，并启用确定性的性能夹具。

普通 Debug/Release 构建的 `BENCHMARK_FIXTURES_ENABLED` 为 `false`，不会把合成歌曲带入用户环境。

## 3. 10,000 首混合资料库

性能夹具生成：

```text
总数            10,000
MediaStore 风格  7,000
SAF 风格         3,000
艺术家             250
专辑             1,000
文件夹              80
```

条目具有稳定 ID、路径、时长和日期，保证不同 CI 运行使用同一排序和聚合输入。SAF 风格条目使用负数媒体 ID，覆盖双来源列表、路径展示和稳定 LazyColumn key。

## 4. Profile 生成

```bash
./gradlew --no-daemon :app:generateBaselineProfile
```

Profile 旅程覆盖：

- 冷启动；
- 等待 10,000 首资料库进入稳定状态；
- 资料库双向滚动；
- 设置页进入与滚动。

生成规则只保留应用包名，写入 `modern-app/src/main/generated/baselineProfiles/`，并通过 `profileinstaller` 为兼容设备提供安装支持。

## 5. Macrobenchmark 场景

### 冷启动

`StartupBenchmark` 分别运行：

- `CompilationMode.None()`；
- `CompilationMode.Partial(BaselineProfileMode.Require)`。

两种模式均使用冷启动并重复测量，不把 Profile 存在等同于 Profile 有效。

### 设置页滚动

`SettingsScrollBenchmark` 使用 FrameTimingMetric，覆盖较长设置页的双向滚动。

### 10,000 首列表滚动

`LargeLibraryScrollBenchmark` 在 Profile 编译模式下等待完整资料库出现，再执行多轮上下滚动并记录 FrameTiming。

## 6. 强制停止恢复

`PlaybackProcessRecoveryTest` 在 benchmark 变体中：

1. 清空目标应用数据并授予媒体权限；
2. 启动 10,000 首资料库；
3. 选择确定歌曲并建立队列；
4. 开启随机和列表循环；
5. 等待持久化提交；
6. 执行 `am force-stop`；
7. 重启应用；
8. 验证曲目、随机和循环状态恢复。

该测试使用真实进程边界，而不是只创建第二个 Repository 对象。

## 7. CI

Pull Request、默认分支和手动工作流依次执行：

```text
构建门禁
  ↓
API 35 应用仪器化测试
  ↓
Baseline / Startup Profile 生成
  ↓
强制停止恢复 + Macrobenchmark smoke suite
```

性能 artifact 保留 14 天，包含 Profile、benchmark JSON、AndroidTest XML 和 HTML 报告。

## 8. 结果解释

AOSP ATD 模拟设备适合检测相对回归和功能性门禁，但不应作为最终毫秒阈值。正式发布还需要在代表性低端、中端和高端真机上建立：

- 冷/暖启动 P50、P95；
- 首帧和完全可交互时间；
- 滚动帧时间和慢帧比例；
- PSS、Java/Kotlin heap 和原生内存峰值；
- GC 次数和暂停时间；
- 10,000 首以上资料库首次加载与增量刷新耗时；
- 数千首歌单、超大队列和长歌词压力结果。

只有建立目标硬件基线后，才适合将具体数值设置为发布阻断阈值。
