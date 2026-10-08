# 内嵌同步歌词

## 目标与来源优先级

Alpha22 恢复旧版本已经承诺、但 Compose 重写阶段缺失的音频内嵌同步歌词。歌词解析完全在本地进行，不联网、不修改音频文件，也不重新引入旧 Java/XML 模块使用的 `jaudiotagger` 运行链。

当前来源顺序固定为：

```text
用户手动导入的 LRC
  ↓ 不存在或损坏
音频文件内嵌同步歌词
  ↓ 不存在或不含时间标签
无歌词状态
```

手动导入用于显式覆盖音频标签。只有导入文件显示“移除导入”；内嵌标签保持只读。移除导入后，同一首歌会立即重新解析或读取缓存中的内嵌歌词。

## 支持格式

### ID3v2

支持 ID3v2.2、v2.3 与 v2.4：

- `USLT` / `ULT`：字段文本本身包含 LRC 时间标签；
- `SYLT` / `SLT`：时间戳格式为毫秒；
- `TXXX` / `TXX`：描述符为 `LYRICS`、`SYNCEDLYRICS`、`UNSYNCEDLYRICS`、`LYRIC` 或 `LRC`，且内容包含 LRC 时间标签；
- `COMM` / `COM`：同样只接受明确的歌词描述符和同步内容。

文本编码支持 ISO-8859-1、UTF-16 BOM、UTF-16BE 与 UTF-8。解析器处理 ID3 unsynchronization，并跳过压缩或加密 frame。

### FLAC

支持 Vorbis Comment 中上述歌词字段。只有包含可解析时间标签的内容才进入同步歌词界面；普通无时间轴文本不会被误显示为同步歌词。

## 安全与性能边界

解析仅打开 `ContentResolver` 只读流，不申请共享存储写权限：

- ID3 tag 上限 16 MiB；
- FLAC metadata 扫描上限 32 MiB；
- 单个歌词文本约 2 MiB；
- Vorbis comment 数量上限 10,000；
- 成功解析结果使用 24 项进程内 LRU；
- 损坏、截断、未知编码、加密或不支持的标签降级为“无歌词”，不影响 Media3 播放。

缓存键包含媒体 ID 与内容 URI。应用重启、URI 变化或曲目身份变化会重新解析；缓存不写入磁盘。

## UI 语义

歌词页明确显示来源，例如：

```text
来源：已导入 LRC
来源：音频内嵌 · ID3 USLT
来源：音频内嵌 · ID3 SYLT
来源：音频内嵌 · FLAC 标签
```

导入按钮在不同状态下显示“导入歌词”“导入覆盖”或“替换歌词”，避免用户误以为内嵌标签会被删除。

## 验证

自动化覆盖：

- ID3v2.3 UTF-16 USLT；
- ID3v2.4 SYLT；
- 歌词类 TXXX 与无关 TXXX 隔离；
- FLAC Vorbis Comment；
- 无时间戳文本拒绝；
- 损坏与截断输入容错；
- SYLT 对文本型候选的优先级；
- 导入覆盖、移除后回退与进程内缓存。

Samsung SM-G970U / Android 12 / API 31 还使用真实有效 MP3 完成了以下端到端路径：

```text
ID3 USLT + LRC
  → MediaScanner / MediaStore
  → Media3 播放队列
  → 当前 content URI
  → EmbeddedLyricsExtractor
  → Compose 歌词页与时间轴
```

## 尚未覆盖

- M4A/MP4 `©lyr` 等容器标签；
- Ogg/Opus Vorbis Comment 容器；
- 没有时间标签的纯文本歌词展示；
- 同目录 `.lrc` / `.txt` 自动发现；
- 双语、逐字和增强 LRC 编辑。
