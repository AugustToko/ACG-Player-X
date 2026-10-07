# Samsung Android 12 / API 31 实机验收

## 基线

- 设备：Samsung SM-G970U
- 系统：Android 12 / API 31
- 分支：`fix/samsung-api31-benchmark-gate`
- 版本：`2.0.0-alpha21` / versionCode 220
- 目标包：`top.geek_studio.chenlongcould.musicplayer`

该门禁补充 API 35 AOSP ATD，重点覆盖旧媒体权限、三星输入法、One UI SystemUI、release-like R8、真实物理 GPU 和强制停止恢复。它不是 SD 卡、USB、云盘 Provider 或蓝牙音频路由的替代测试。

## 修复内容

1. API 33+ 使用 `READ_MEDIA_AUDIO`，API 32 及以下使用 `READ_EXTERNAL_STORAGE`。
2. 恢复测试不再用 Back 关闭三星输入法；改用 IME Search/Enter，并在 Compose 重组后重新查询 accessibility node。
3. 显式约束 `androidx.work:work-runtime-ktx:2.11.2`，替换 Glance 1.2.0 带入的旧 WorkManager 2.7.1，保证 AGP 9.4/R8 release-like 构建与 API 31 启动。
4. benchmark 变体注册只读 `BenchmarkMediaProvider`，为 10,000 首合成歌曲提供确定的 WAV 与 PNG，消除缺失 authority 的日志风暴并让 Media3 播放链真实打开文件描述符。

## 主机门禁

- Android Lint：通过
- JVM 单元测试：117 通过，0 失败，0 跳过
- Debug、AndroidTest、benchmark APK：通过
- `:app:assembleBenchmark`：通过，包括 R8 与 `lintVital`

## 设备门禁

- 应用仪器化：32/32 通过
- 播放强停恢复：通过；附加稳定性循环 3/3 通过
- 合成媒体播放：MediaSession 为 PLAYING，队列 10,000，元数据为 `Benchmark Song 00001`
- 物理设备日志中未发现目标应用的 FATAL、ANR、WorkDatabase 启动失败、Media3 playback exception 或 benchmark authority 缺失

## 性能观察值

| 路径 | 结果 |
| --- | --- |
| release-like 干净冷启动 | 233 ms 单次 `am start -W` 观察 |
| 冷启动，无编译 | min/median/max 291.6 / 317.1 / 444.1 ms |
| 冷启动，Baseline Profile | min/median/max 302.0 / 361.9 / 383.0 ms |
| 设置页 CPU 帧时 | P50/P90/P95/P99 5.5 / 7.2 / 8.0 / 9.8 ms |
| 设置页 Frame overrun | P99 -5.0 ms |
| 10,000 首 CPU 帧时 | P50/P90/P95/P99 5.7 / 8.5 / 9.5 / 11.4 ms |
| 10,000 首 Frame overrun | P99 -3.5 ms |

Baseline Profile 在该次小样本中没有改善启动中位数，但降低了最慢样本；不能据此建立跨设备收益结论。

## 尚未覆盖

- 外置 SD 卡和 USB OTG 拔插
- 云盘 DocumentsProvider 离线、权限撤销和文件移动
- 真实蓝牙耳机、车机、音频焦点竞争和路由切换
- Doze、三星后台冻结、低内存杀进程和系统重启
- 30 分钟以上循环、PSS/GC 趋势与泄漏判定
