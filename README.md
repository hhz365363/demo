# 断点续传文件上传

一个把「分片上传 + 断点续传 + 完整性校验」讲清楚的最小可运行实现。

不是文件网盘，也没有登录、权限、缩略图那一套。我只想把一件事做扎实：**一个 2GB 的文件传到 60% 时断网了，重新打开页面点继续，前面那 60% 不用重传，并且最终落到磁盘上的文件和原文件一个字节都不差。**

前端是原生 HTML + JS，没有构建步骤，没有框架，打开就能看。后端 Spring Boot 3 + MyBatis + SQLite，单机跑起来不依赖任何外部服务。

---

## 目录

- [它是怎么工作的](#它是怎么工作的)
- [功能](#功能)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [接口](#接口)
- [数据模型](#数据模型)
- [几个实现上的取舍](#几个实现上的取舍)
- [已知问题与后续计划](#已知问题与后续计划)
- [目录结构](#目录结构)

---

## 它是怎么工作的

一次完整的上传分四步，客户端和服务端各自负责一半：

```
浏览器                                    服务端
  |                                         |
  |  1. POST /newTask                       |
  |     {fileName, fileSize}                |
  |---------------------------------------->|  建目录、生成随机文件名
  |<----------------------------------------|  算出分片总数，落一条 upload_task
  |     taskId, totalCount                  |
  |                                         |
  |  2. 按 1MB 切片，逐片 POST /upload      |
  |     [file, hash, taskId, chunkIndex]    |
  |---------------------------------------->|  校验这一片的 SHA-256
  |                                         |  写盘 -> 写 file_chunk
  |<----------------------------------------|  OK
  |                                         |
  |  （断网 / 关页面 / 点暂停，这里停住）   |
  |                                         |
  |  3. POST /keep                          |
  |     taskId                              |
  |---------------------------------------->|  总数 - 已存在的序号
  |<----------------------------------------|  [7, 8, 9, ...]  只要这些
  |                                         |
  |  4. POST /merge                         |
  |     {taskId, 整个文件的 hash}           |
  |---------------------------------------->|  按序号顺序拼接
  |                                         |  边拼边算整文件 SHA-256
  |                                         |  比对 -> 写 user_file
  |<----------------------------------------|  fileId
```

关键点在于：**服务端永远以数据库里 `file_chunk` 的记录为准判断「哪一片收到过」**，而不是相信客户端说「我传到第 30 片了」。客户端可以随便丢状态，只要 `taskId` 还在，问一次 `/keep` 就能接着传。

`/merge` 也不要求分片全齐才响应：缺片时它返回 200 和一份 `missList`，前端补完再 merge 一次。这样「边传边丢片」这种网络抖动导致的情况，走的是正常流程，不需要靠异常码去猜。

## 功能

- **分片上传**：默认 1MB 一片，切片在前端用 `File.slice()` 完成，后端不接触原文件
- **断点续传**：`/keep` 返回缺失序号，只补传缺的部分；暂停后点继续、甚至换一台机器拿到 `taskId` 也能接
- **两级完整性校验**：
  - 每片上传时校验该片的 SHA-256，防止半截数据落盘
  - 合并时对拼接结果再算一次整文件 SHA-256，和前端给的值比对，不一致直接拒绝
- **可中断**：前端用 `AbortController` 掐断正在传输的请求，不是等它传完再停
- **大文件不占内存**：整文件 hash 在 Web Worker 里按 4MB 增量计算，内存占用恒定；主线程不卡
- **取消与清理**：`/close` 把任务标记为已取消，定时任务每小时把取消任务的磁盘目录和数据库记录一起清掉
- **无外部依赖**：SQLite 单文件数据库，`mvn spring-boot:run` 就能跑

## 技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 后端 | Java 17 + Spring Boot 3.0 | 只用了 spring-boot-starter-web |
| 持久层 | MyBatis 3.0.3 | 简单 SQL 用注解，`in (...)` 这种动态语句放 XML |
| 数据库 | SQLite 3.45 | 单文件，省掉装 MySQL 的步骤 |
| 前端 | 原生 HTML/CSS/JS | 无框架、无构建、无 npm |
| 校验 | SHA-256 | 前端 `crypto.subtle`（分片）+ 手写增量实现（整文件），后端 `MessageDigest` |

## 快速开始

### 环境

- JDK 17 或更高（`pom.xml` 里 `maven.compiler.source=17`）
- Maven（用 IDEA 自带的 Maven 面板也行，项目没有附带 wrapper）

### 1. 改掉写死的存储路径

`src/main/java/org/example/Main.java`：

```java
public static final String FILE_PATH = "C:\\Users\\admin\\Desktop\\files";
```

这个路径是我本地调试时留下的，**必须改成你机器上存在的目录**，否则 `newTask` 会在 `Files.createDirectories` 那里直接抛异常。目录本身不用提前建，代码会创建。

### 2. 建表

仓库里带了 `sql.txt`，三张表都在里面。用 SQLite 命令行：

```bash
sqlite3 demo.db < sql.txt
```

或者直接把内容贴进 DBeaver / IDEA 的数据库工具执行。表都写了 `IF NOT EXISTS`，重复执行没关系。

### 3. 启动

```bash
mvn spring-boot:run
```

或者在 IDEA 里直接跑 `org.example.Main`。默认端口 8080。

### 4. 打开测试页

```
http://localhost:8080/index.html
```

选文件 → 开始上传 → 中途点暂停 → 点继续。页面右侧的日志面板会把每一次请求和响应原样打出来，包括每一片的 hash 和合并结果，方便对照。

## 接口

所有接口都是 POST。统一响应体：

```json
{ "state": 1, "msg": null, "data": {} }
```

`state` 为 `1` 表示成功，`0` 表示失败（`msg` 里有原因）。

| 接口 | 参数 | 返回 | 说明 |
|---|---|---|---|
| `/newTask` | JSON：`userId`、`originalFileName`、`fileSize` | `UploadTask` | 创建上传任务，返回 `id` 和 `totalCount` |
| `/keep` | form：`taskId` | `List<Long>` | 返回还没收到的分片序号（从 1 开始） |
| `/upload` | multipart：`file`、`hash`、`taskId`、`chunkIndex` | 空 | 上传单个分片，`hash` 是该片的 SHA-256 |
| `/merge` | JSON：`taskId`、`hash` | `{fileId, missList}` | 合并。`missList` 非空表示还缺片，`fileId` 为 null |
| `/close` | form：`taskId` | 空 | 取消任务，标记为待清理 |
| `/getUnfinishedTaskList` | form：`userId` | `List<UploadTask>` | 拉该用户所有 `state=0` 的任务 |

几个约定：

- `chunkIndex` **从 1 开始**，不是 0。`/keep` 返回的、`/merge` 返回的 `missList` 都是 1-based。
- `/upload` 是幂等的：同一个 `taskId + chunkIndex` 重复提交，第二次会直接跳过（数据库里有唯一索引兜着）。
- `/merge` 的 `hash` 是**整个文件**的 SHA-256，不是某一片的。
- 任务状态只有三个：`0` 未完成、`1` 已取消、`2` 已完成。已完成或已取消的任务再调 `/keep`、`/upload`、`/merge` 都会抛「上传任务不存在 / 上传任务已完成」。

## 数据模型

三张表，关系很直白：一个任务 → 多个分片，任务完成后转成一条文件记录。

```
upload_task (上传任务)          user_file (已保存的文件)
  id                              id
  user_id                         user_id
  original_file_name              original_file_name      合并成功后
  local_file_name                 local_file_name         由 upload_task
  file_size                       file_size               拷贝过来
  local_parent                    local_parent
  local_path                      local_path
  hash                            hash                    整文件 SHA-256
  state  0未完成/1取消/2完成       state  0已保存/1已删除
  total_count                     total_count
       |
       | 1 : n
       v
  file_chunk (已收到的分片)
    id
    task_id
    hash            该片的 SHA-256
    chunk_index     1-based
    size
    UNIQUE(task_id, chunk_index)
```

磁盘上的布局：

```
{FILE_PATH}/{随机8位}/
    ├── {随机UUID}      ← 合并后的完整文件
    ├── 1               ← 分片，文件名就是序号
    ├── 2
    └── ...
```

分片合并完成后，`1..N` 这些分片文件会丢给线程池异步删掉，不阻塞 `/merge` 的响应。

## 几个实现上的取舍

这部分是我写的时候真正纠结过的地方，记下来免得以后自己忘了为什么这么写。

**1. 前端串行传分片，没做并发**

并发能压满带宽，但会让「暂停 → 继续」变得很难说清楚：同时有 6 个分片在飞，暂停时要等它们全部 abort，还要维护一个重试队列。串行的模型是「一片传完再传下一片」，暂停点只有一个，`/keep` 的语义天然对齐。要提速的话正确做法是加并发 + 每片独立重试，但我先保证正确性。

**2. 整文件 hash 是手写的 SHA-256，没用 `crypto.subtle`**

`crypto.subtle.digest()` 不支持流式，必须把整个文件读成一个 `ArrayBuffer`。传 2GB 文件时这一下就是 2GB 内存，标签页直接崩。

所以 `worker.js` 里手写了一份增量 SHA-256：维护 8 个 32 位中间值 + 64 字节余量缓冲，每次只从文件里 `slice` 4MB 进内存。内存占用是恒定的，主线程也不卡。

代价是要自己处理补位（padding）和长度编码，输出必须和 Java 的 `MessageDigest + HexFormat` 逐字节一致，否则每次 merge 都会报完整性校验失败。这段代码我对着 Java 的输出验过。

**3. 先写磁盘，再写数据库**

`/upload` 的顺序是 `Files.write(...)` 然后 `insertChunk(...)`，中间没有事务能覆盖（文件系统不参与事务）。

我选这个顺序是因为两种失败场景不对称：

- 写盘成功、写库失败 → 库里说没收到，下次 `/keep` 会重传这一片，覆盖写一遍，结果正确
- 写库成功、写盘失败 → 库里说收到了，合并时读不到文件，文件永久损坏

所以宁可留孤儿文件，也不能留孤儿记录。

**4. `/merge` 缺片时返回 200，不是 4xx**

「缺片」在网络抖动的场景下是常态，不是异常。如果返回错误码，前端每次都要去解析错误信息里的序号列表，很别扭。现在的写法是 `{fileId: null, missList: [7,8]}`，前端读 `missList` 补完再 merge，代码线性直白。

前端另外加了 `MAX_MERGE_ROUND = 10` 的上限，防止后端某个 bug 导致一直返回同样的 `missList`，页面无限请求。

**5. 分片存了 hash，但没做秒传**

`file_chunk.hash` 现在只用来在 `/upload` 时校验这一片的内容有没有传坏，**没有**拿它去查重。

真正的「秒传」应该是：客户端先把整文件 hash 算出来，问服务端有没有同名 hash 的 `user_file`，有就只插一条记录、跳过所有分片。字段和结构都已经留好了（`user_file.hash` 有值可查），差一个 `/check` 接口。这是我下一版要做的第一件事，也就没为了凑功能现在硬塞进去。

## 已知问题与后续计划

按严重程度排，不掩饰：

1. **存储路径写死**，且用了 `\\` 拼路径，只能在 Windows 上跑。正确做法是配到 `application.yml` 里，用 `Path.of()` 拼。
2. **没有登录**，`userId` 前后端都写死 `1`。所有任务归属同一个用户。
3. **没有秒传**，同一个人传两次同一个文件，磁盘上就是两份。
4. **前端刷新后 `taskId` 会丢**。`currentTask` 只存在内存里，刷新页面得重新选文件、重新建任务。`/getUnfinishedTaskList` 这个接口已经写好了，但页面还没接上去。这是目前体验上最大的缺口。
5. **分片大小和 multipart 限制是联动的**。`application.yml` 里 `max-file-size: 1MB` 刚好卡住 1MB 的分片，谁把 `Main.CHUNK_SIZE` 调大而忘了改这里，就会收到 413。
6. **没有下载和删除接口**。`user_file` 里 `state=1`（已删除）的记录只能靠定时任务清理，还没有触发删除的入口。
7. **单文件串行上传，多文件也不能同时进行**，`running` 标记是全局的。
8. **定时任务两小时级联清理没有做幂等保护**，如果磁盘删除抛异常，整批任务会中断在抛出点。

后续计划：秒传 → 并发分片 + 单分片重试 → 刷新页面恢复任务 → 分片大小可配 → 断点续传的 e2e 测试。

## 目录结构

```
.
├── pom.xml
├── sql.txt                       建表语句
├── demo.db                       SQLite 库（测试数据，可删）
└── src/main
    ├── java/org/example
    │   ├── Main.java             启动类，CHUNK_SIZE / FILE_PATH / USERID 常量
    │   ├── controller             TaskController，六个接口
    │   ├── service
    │   │   ├── TaskService        上传、续传、合并的主流程
    │   │   ├── FileChunkService   分片记录与清理
    │   │   └── UserFileService    已保存文件
    │   ├── dao                    MyBatis Mapper（简单 SQL 用注解）
    │   ├── om
    │   │   ├── dto                入参：NewTaskDTO / UploadDTO / MergeDTO
    │   │   ├── vo                 出参：ResultVO / MergeVO
    │   │   ├── schema             实体：UploadTask / FileChunk / UserFile
    │   │   └── enums              状态枚举
    │   ├── scheduled              每小时清理：取消的任务、已删除的文件
    │   ├── config                线程池、全局异常处理
    │   └── util                  随机串、16 进制与摘要
    └── resources
        ├── application.yml
        ├── mapper/*.xml          动态 SQL（in 查询）
        └── static
            ├── index.html        测试页：选文件、开始、暂停、继续、日志
            └── worker.js         增量 SHA-256，算整个文件的 hash
```

---

写这个项目的起因是想搞明白「断点续传」到底难在哪。结论是：难的不是切片，也不是合并，而是**服务端凭什么相信客户端说自己传了 60%**。想清楚这一点，剩下的都是体力活。
