# Alpha20 统计导入准备流程修复（2026-09-21）

基线分支：`modernize/compose-ui-2.0`，PR #3。
基线提交：`d3baa89d7ef2c0cd9717fb384fc375117dfd6f11`。
保持 `2.0.0-alpha20` / versionCode 219；本批是既有功能修复，不另起重写分支、不合入 master。

## 源码确认的问题

`StatisticsViewModel.prepareImport()` 原先在 `isRefreshingLibraryMetadata` 为 true 时直接返回。
系统选择器打开期间，权限、目录列表或总曲目数可能改变并启动一次元数据扫描。即使打开选择器时按钮可用，返回的 URI 仍可能被上述分支丢弃。
此外，同一元数据请求在扫描尚未完成时会取消并重启扫描；扫描失效后还可能使用旧预览确认导入。

## 新行为与写入边界

- 文件选择结果立即进入读取阶段，不因元数据扫描而丢弃。仍复用原 JSON 读取器的 4 MB / 20,000 项限制。
- 解析后的文档保留在 ViewModel 生命周期内；只有文档与成功的元数据快照都就绪时才生成预览。
- 明确区分读取、等待元数据、匹配、预览、元数据失败、文件/匹配失败。
- 扫描失败不是空音乐库。失败时不做离线判定、不允许确认，用户可重试或取消；重试复用已解析的文档。
- 取消/更换文件和重新扫描都会使旧代次失效。即使旧后台任务迟到，也不能恢复已取消预览或覆盖当前预览。
- 相同扫描请求复用正在运行的任务；计数只用作缓存失效提示，不以“两个计数相等”证明来源身份。
- 匹配计算移到 `Dispatchers.Default`。文件 I/O 与统计落库继续使用原数据层。
- 默认仍为幂等合并，保留既有完全替换和可选离线 ID 行为。准备完成不会自动写入。
- 明确点击确认才建立写入事务；确认后锁定所选预览、模式与 ID，防止重复点击提交。扫描在确认前失效必须重新预览。
- 清理统计、导出和导入准备互斥，不改动原有清理范围/epoch 保护。收藏、歌单、歌词和音频文件不在本补丁写入范围。
- 待处理文档只跨 Activity 配置重建保留，不声称跨进程/系统重启恢复。进程死亡后需重新选文件并确认。

## 测试

### 不依赖 Android SDK 的真实 Kotlin/JVM 回归

```bash
bash tools/check_statistics_import_preparation.sh
```

执行生产 `ImportPreviewPreparation` 的 16 个确定性场景，包括文档/扫描两种完成顺序、首次扫描前选择、空库成功、扫描失败、重试、取消、替换请求、过期扫描/匹配、重复通知和错误恢复。
同一组场景通过 `ImportPreviewPreparationTest` 参数化接入 Gradle JUnit；没有用桩替代状态机实现。

本批提交前已在本地 Kotlin/JVM 1.9.0 / JRE 21 上执行 16/16 场景通过，并通过 Bash/Python 语法检查。
这不等于完整 Android 构建或设备测试通过；后者必须读取本提交对应的 CI 结果。

### Android ViewModel + Compose 回归

新增 `StatisticsImportPreparationInstrumentedTest`，共 6 个测试：

1. 文件选择返回时扫描未完成，随后出现真实预览；提前确认不写入，明确确认仅写入一次。
2. 等待时取消，扫描结束后不复活预览。
3. 扫描失败重试，不再次选择或读取文件。
4. 扫描变更立即使旧预览失效，使用新媒体 ID 重建。
5. 读取中取消，迟到结果被忽略。
6. 同一请求多次触发只启动一次扫描。

这些测试使用真实 ViewModel、Compose 状态和对话框，并通过可控的挂起 I/O 边界固定竞态顺序。
它们不是系统 DocumentsUI、真实 SD 卡、云盘 Provider、OEM 或进程死亡验收。既有仓库文件往返与隐私测试继续保留。

```bash
./gradlew --no-daemon :app:lintDebug :app:testDebugUnitTest \
  :app:assembleDebug :app:assembleDebugAndroidTest :benchmark:assembleBenchmarkBenchmark
./gradlew --no-daemon --info :app:pixel2Api35DebugAndroidTest
```

既有 Profile、进程恢复和大型库 smoke suite 仍保持，不增加 skip 或 continue-on-error。

## 证据身份

CI 三个 job 在构建前记录实际检出 commit、tree、父提交、run/attempt/job 及变更源码 blob/SHA-256 到 `build/source-identity.json`。
若记录目标存在未提交改动，直接失败。身份记录与各层报告一并上传。

PR 工作流通常检出合并测试提交，不应把 `head_sha`、实际 checkout SHA 和旧运行日志混为一谈。
只有新运行的身份记录、测试 XML 和 job 结果一致，才可宣称本补丁通过相应层级。旧运行成功或 Push 仅构建成功不能替代设备/性能结果。

## 未包含的后续工作

真实系统文件选择器与授权撤销矩阵；跨进程导入草稿；大型匹配索引优化；更多统计可视化；统计组件从设置 LazyColumn 中提升为独立页面级导航。
