# 收听统计清理、导入与异步写入边界

## 2026-09-21 接手基线

本次从 `modernize/compose-ui-2.0` 的 `d3baa89d7ef2c0cd9717fb384fc375117dfd6f11` 继续，不回退到 Alpha16/Alpha18。活动模块已为 `2.0.0-alpha20` / versionCode 219，统计卡片、JSON/CSV 导出、JSON 导入预览、幂等合并/完全替换和隐私清理均已存在。本次保持版本号，修复 Alpha20 的验收和并发正确性，不重复宣称这些旧功能是本次新增。

基线 PR run `35523671694`：build 成功，API 35 失败，performance 被跳过。下载的原始 JUnit XML 为 17 项、1 项失败；失败为 `PlaybackStatisticsImportRepositoryInstrumentedTest.initializationError`，原因是 `futureSchemaIsRejected()` 的表达式体推断为非 void 返回值。因此此前整个统计导入测试类没有正常执行，不能把 AndroidTest APK 编译成功等同于设备测试通过。

## 修复一：JUnit 4 方法契约

两个 `runBlocking` 测试显式声明 `: Unit`，保留未来 schema 必须拒绝的原始断言，不改 expected exception、不跳过测试。

## 修复二：清理前的采样不得写回

旧流程存在确定的交错：

1. PlaybackService 已累积尚未写盘的收听增量，或已捕获等待 IO 的增量。
2. 用户通过 StatisticsViewModel 清空 DataStore。
3. 播放服务继续写入旧增量，可能重建刚被清除的次数、完成记录或位置。

现在 DataStore 同时保存两种独立、不透明的 generation：

- `playback_stats_generation`：统计清理及 REPLACE 导入时更新。
- `recent_media_generation`：最近播放清理时更新。

清空数据和更新 generation 在同一个 `edit` 事务中完成。延迟写入捕获旧 generation，写入时在 `edit` 内重新比较。不能在事务外检查后再写，否则仍存在竞态。

| 操作 | 最近播放旧写入 | 统计旧写入 | 采样器缓存 |
| --- | --- | --- | --- |
| 仅清空最近播放 | 拒绝 | 仍接受 | 保留统计会话 |
| 仅清空统计 | 仍接受 | 拒绝 | 下一次采样丢弃旧会话 |
| 两项都清空 | 拒绝 | 拒绝 | 下一次采样丢弃旧会话 |
| 完全替换导入 REPLACE | 仍接受 | 拒绝 | 丢弃替换前会话 |
| 幂等合并 MERGE | 仍接受 | 仍接受 | 不重置 |
| 不选择任何清理项 | 不变 | 不变 | 不变 |

`PlaybackAnalyticsSampler` 在播放器线程生成不可变批次，携带 generation 和采样时刻。Media3 属性仍只在主线程读取；DataStore 写入在 IO 上进行。清理之后发现 generation 变化时，会直接替换旧 tracker，不能调用 finish 将旧证据重新 flush。不会把清理动作本身当作一次新的播放入口。

`readListeningDataGeneration()` 直接读取 `dataStore.data.first()`；不能复用用于 UI 降级的 emptyPreferences fallback，以免磁盘读取失败时伪造 generation。

## 用户可见语义与限制

- 清理历史不等于永久关闭收听统计。正在播放时，清理之后新产生的自然收听可以继续累计。
- 清空统计不清空 Media3 播放队列，不停止播放，也不清空独立 PlaybackStateStore 的播放上下文。统计里的继续听位置与播放器全局恢复位置不是同一份数据。
- 本次不改变收藏、歌单、歌词、SAF 授权或音频文件。
- 导出的 JSON/CSV 文件不会因清理应用内部数据自动删除，需用户自行管理已导出副本。
- generation 是同一个应用进程中共享 DataStore 的持久化写入边界，不是新增跨进程 DataStore 支持。
- Repository 的 `expectedGeneration = null` 仅供即时新事件/现有同步入口兼容；任何新的异步采样生产者都必须在捕获证据时读取并传入 generation。PlaybackService 已完整接线。
- 不宣称修复断电、存储失败重试、完整会话持久化或任意设备时序；这些仍需要独立验收。

## 回归测试

新增 JVM `PlaybackAnalyticsSamplerTest` 7 项：清理丢弃未提交增量、最近清理不影响会话、替换导入丢弃旧完成证据、真实重复播放计数、不可变批次代际、独立清理范围、暂停时清理不产生播放。

新增 API 35 `ListeningDataGenerationInstrumentedTest` 8 项：用 CompletableDeferred 控制延迟写入，分别验证双清理、两个单范围清理、REPLACE、MERGE、Repository 重建后的 generation 读取、新事件接受、空操作及旧 clearRecent 入口。使用真实 Preferences DataStore，不用 sleep 猜测竞争时机。原有收藏保留测试继续执行。

本地验证只编译真实纯 Kotlin generation/sampler/tracker 和生产 DTO，使用 kotlin.test 执行上述 7 个采样回归；它不是 Android 构建或 DataStore 设备验证。完整验证以本次提交对应的 Actions job 和原始 XML 为准，不复用祖先提交的绿色结果。

```bash
./gradlew --no-daemon :app:lintDebug :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :benchmark:assembleBenchmarkBenchmark
./gradlew --no-daemon --info :app:pixel2Api35DebugAndroidTest
```

触及播放服务，需继续执行现有 Profile、进程恢复和 10k 性能工作流。未完成或被跳过的作业不能标为通过。托管设备合成 10k 数据及性能 smoke 也不等同于真实 10k 文件扫描或目标真机性能达标。

## 参考

- JUnit 4 Test 方法契约：https://junit.org/junit4/javadoc/latest/org/junit/Test.html
- DataStore 原子事务与 data.first：https://developer.android.com/reference/androidx/datastore/core/DataStore
- Media3 线程契约：https://developer.android.com/media/media3/exoplayer/hello-world
