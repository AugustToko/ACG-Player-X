# ACG Player X 迁移矩阵

状态：**完成**＝已进入活动 Compose 模块并由默认 CI 构建；**部分完成**＝核心可用但仍缺发布级验证；**未开始**＝尚未迁入。

## 应用基础

| 能力 | 状态 | 当前实现 | 后续 |
| --- | --- | --- | --- |
| 构建工具链 | 完成 | Kotlin DSL、Version Catalog、AGP 9.4、Gradle 9.6、JDK 17 | 跟随稳定版维护 |
| 主 UI | 完成 | Compose Material 3、edge-to-edge | 截图与无障碍测试 |
| 手机/大屏导航 | 完成 | Bottom Navigation / Navigation Rail | 折叠屏双栏 |
| 主路由 | 完成 | `MainUiState.destination` | 进程恢复与深链 |
| 歌单路由 | 部分完成 | Compose shell + PlaylistViewModel | 统一类型安全导航 |
| 动态快捷入口 | 完成 | 收藏、最近、新添加、常听 | OEM Launcher 验证 |
| 主题 | 完成 | 动态配色、亮/暗/系统、DataStore | 高对比度 |

## 音乐库

| 能力 | 状态 | 当前实现 | 后续 |
| --- | --- | --- | --- |
| MediaStore 扫描 | 完成 | 分版本查询、DISPLAY_NAME、DATE_ADDED | 10,000 首基准 |
| SAF 授权目录 | 完成 | OpenDocumentTree、持久只读 URI | Provider 实机矩阵 |
| SAF 递归扫描 | 完成 | BFS、20,000 项、32 层保护 | 增量索引与磁盘缓存 |
| 双来源合并 | 完成 | URI/指纹去重，MediaStore 优先 | 冲突样本验收 |
| 歌曲/专辑/艺术家/文件夹 | 完成 | Compose 聚合列表 | 快速索引与多选 |
| 联合搜索 | 完成 | 标题、艺术家、专辑、文件与目录 | 拼音和模糊排序 |
| 自动刷新 | 完成 | ContentObserver + 去抖 | 增量更新 |
| 失效授权降级 | 完成 | 保留注册表并提示 | 系统设置深链 |

## 自定义歌单

| 能力 | 状态 | 当前实现 | 后续 |
| --- | --- | --- | --- |
| 持久化 | 完成 | Preferences DataStore、有序媒体 ID | 大型歌单基准 |
| 创建/重命名/删除 | 完成 | Compose 对话框与同名校验 | 删除歌单撤销 |
| MediaStore + SAF 混合 | 完成 | 正数/稳定负数 ID | ID 迁移文档 |
| 添加/搜索/播放 | 完成 | 单曲、当前结果、顺序/随机 | 选择器勾选式多选 |
| 单曲顺序编辑 | 完成 | 上移/下移 + 撤销 | 无 |
| 批量编辑 | 完成 | 搜索、多选、置顶/置底/移出 | 大歌单选择性能 |
| 任意位置拖拽 | 完成 | 长按拖动柄、边缘自动滚动、草稿后原子保存 | 实机手势测试 |
| 清空/失效项清理 | 完成 | 二次确认 + 撤销 | 离线来源分类 |
| 不可用来源保序 | 完成 | 保留 ID 和原始位置 | 失效项详情 |
| Snackbar 撤销 | 完成 | 进程内单步完整顺序快照 | 持久历史可选 |

## M3U / M3U8

| 能力 | 状态 | 当前实现 | 后续 |
| --- | --- | --- | --- |
| 文件读取与编码 | 完成 | OpenDocument、UTF-8/BOM、GB18030、4 MB | Provider 样本 |
| 标准解析 | 完成 | EXTM3U、PLAYLIST、EXTINF、20,000 项 | 更多第三方样本 |
| 分层自动匹配 | 完成 | 校验后的 ID/URI、文件名/目录、元数据 | 大库性能基准 |
| 跨设备冲突保护 | 完成 | 文件名/元数据联合校验复用 ID/URI | 实机迁移样本 |
| 导入逐条预览 | 完成 | 自动/离线/歧义/未匹配/重复状态 | 截图测试 |
| 手工映射 | 完成 | 候选选择、完整库搜索、排除/恢复 | 语义搜索可选 |
| 最终顺序与去重 | 完成 | 原始索引排序，媒体 ID 首次出现优先 | 导入审计导出 |
| M3U8 导出 | 完成 | CreateDocument、EXTINF、ACG 注释、离线占位 | 相对路径策略 |
| 同名导入 | 完成 | 递增后缀 | 用户自定义冲突策略 |
| 旧 MediaStore Playlist | 未开始 | 无 | 只读导入评估 |

## 智能音乐库

| 能力 | 状态 | 当前实现 | 后续 |
| --- | --- | --- | --- |
| 收藏/最近播放 | 完成 | DataStore ID 集合与顺序 | 批量操作 |
| 播放次数/最后播放 | 完成 | PlaybackStats | 完播阈值 |
| 最近添加/最常播放/未播放 | 完成 | 稳定排序与当前库求差 | 时间窗口规则 |
| 规则智能列表 | 未开始 | 无 | 完成度、时长、目录、标签组合 |

## 播放、封面与歌词

| 能力 | 状态 | 当前实现 | 后续 |
| --- | --- | --- | --- |
| 后台播放/系统控制 | 完成 | Media3 ExoPlayer + MediaSessionService | OEM、车机验证 |
| 队列编辑与恢复 | 完成 | Timeline、Compose 编辑、PlaybackStateStore | 拖拽/批量/撤销 |
| 封面 | 完成 | ContentResolver + 内嵌图片 + LRU | 磁盘缓存与压力测试 |
| LRC 解析/导入/同步 | 完成 | 纯 Kotlin、OpenDocument、内部存储 | 同目录自动匹配 |
| 歌词偏移 | 完成 | 文件 offset + 每曲用户 offset | 数字输入 |
| 歌词编辑/双语/逐字 | 未开始 | 无 | 独立编辑与双时间轴 |
| 均衡器/睡眠定时/无缝 | 未开始 | 无 | Media3/AudioEffect 评估 |

## 旧特色

| 能力 | 状态 | 方向 |
| --- | --- | --- |
| 音频标签编辑 | 未开始 | SAF/MediaStore 写入授权 + 独立数据层 |
| Live2D | 未开始 | AndroidView 隔离层 + 生命周期 |
| 桌面小组件 | 未开始 | Jetpack Glance |
| 购买/Bug Report/远程功能 | 未开始 | 重新评估产品价值与隐私 |

## 工程质量

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 单元测试 | 部分完成 | 路径、SAF、智能库、歌单、拖拽、M3U 预览/映射、LRC |
| Android Lint | 完成 | Push/PR 阻断执行 |
| Debug APK | 完成 | Push/PR 构建 |
| AndroidTest APK | 完成 | Push/PR 编译与打包 |
| 仪器化测试 | 部分完成 | API 35 托管设备：导航、快捷入口、recreate、MediaSession、M3U 预览/文件往返、歌词文件往返 |
| 托管设备诊断 | 完成 | 成功或失败均上传报告 artifact，保留 7 天 |
| 截图测试 | 未开始 | 手机、平板、主题、导入预览、歌词、队列 |
| 性能基准 | 未开始 | Baseline Profile、Macrobenchmark、大型数据集 |
| 发布流水线 | 未开始 | 新凭据、GitHub Secrets、签名与产物 |

## 下一批顺序

1. Baseline Profile、Macrobenchmark 与大型数据基准。
2. 真实 SAF Provider、系统选择器、权限撤销和强制进程回收仪器化测试。
3. M3U 相对路径导出和旧 MediaStore Playlist 只读导入。
4. 规则智能列表与播放完成度。
5. SAF 增量索引、歌词增强、Glance、Live2D 和标签编辑。
