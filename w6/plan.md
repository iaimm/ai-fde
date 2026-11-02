2;[M455


\9# W6 打卡清单：性能与调优周
&gt; 周期：Day 1（六）→ Day 7（五），Day 2（日）复盘日。每天 30 分钟 = 2×15 分钟。
&gt; 说明：清单日期为名义排期，实际按你的进度顺延，顺序不变。

## ⚠️ 本周预期管理（先读三遍）
压测 /api/chat 时，**大头延迟是 kimi 的网络往返**（秒级），不是我们的代码。
本周调优对象是"我们能控制的部分"：检索耗时、JSON 处理、线程模型、前端加载。
LLM 延迟单独记录，不背锅也不抢功。

---

## Day 1 ·（六）埋点：没有测量就没有调优（骨头日）
- [ ] ★ 15min：`ChatController.forwardToLlm` 加三个计时——检索耗时、首 token 到达耗时（TTFT）、总耗时，`System.out.println` 打日志
- [ ] 15min：手测 3 个问题（短/中/长），把三列数字抄进 `w1-demos/性能基线.md`
- 📦 产出：三条耗时的基线数字
- 🆘 降级：只加首 token 和总耗时两项

## Day 2 ·（日）复盘日（轻量）
- [ ] ★ 15min：缺口清单更新 + W5 周报（素材：八坑排错链 + 双 CI 点亮）
- [ ] 15min：22:00 前发周报
- 🆘 降级：只发周报

## Day 3 ·（一）压测基线（骨头日）
- [ ] ★ 15min：装 wrk（`sudo apt install wrk`），压 `/api/hello` 得 RPS 和延迟基线：
      `wrk -t4 -c100 -d30s http://localhost:8080/api/hello`
- [ ] 15min：同样参数压 `/api/chat`（会被 LLM 延迟主导，预期之内），把两组数字对比记入基线文档，写一句"为什么 chat 接口的 wrk 数字没有参考意义"
- 📦 产出：基线数字 + 一段"什么该压、什么不该压"的认知
- 🆘 降级：只压 /api/hello

## Day 4 ·（二）检索性能日
- [ ] ★ 15min：给 `NoteRetriever.top3` 单独计时（Day 1 埋点里已有），观察 chunks 数量对耗时的影响：把 notes 目录复制一份翻倍再测一次
- [ ] 15min：优化一处：打分循环里 `text.toLowerCase()` 每轮重复算——分块加载时预先存小写版本，重测对比，数字记入文档
- 📦 产出：检索优化前后耗时对比
- 🆘 降级：只测不优化，数字记录即可

## Day 5 ·（三）JVM 观察日
- [ ] ★ 15min：启动参数加 `-Xlog:gc:file=gc.log`，重启后端，跑一轮压测，数 GC 次数：`grep "Pause" gc.log | wc -l`
- [ ] 15min：JFR 采样 30 秒：`jcmd &lt;PID&gt; JFR.start settings=profile duration=30s filename=app.jfr`（或 `jconsole` 肉眼观察堆曲线），结论一句话记文档
- 📦 产出：GC 次数 + 一句 JVM 健康结论
- 🆘 降级：只看 GC 日志，JFR 跳过（标记为 W8 债）

## Day 6 ·（四）前端性能日
- [ ] ★ 15min：`npm run build` 看 dist 体积记录 → `frontend/nginx.conf` 开 gzip（`gzip on; gzip_types text/css application/javascript;`）→ 浏览器 F12 Network 看传输体积前后对比
- [ ] 15min：`npx vite build --mode production` 已有的话加 `rollup-plugin-visualizer` 看哪个依赖占体积，结论一句话记文档
- 📦 产出：传输体积前后对比 + 体积构成一句话
- 🆘 降级：只开 gzip 看对比

## Day 7 ·（五）性能报告日（W6 验收）
- [ ] ★ 15min：把本周所有数字汇总成 `docs/performance.md`：调优前 vs 调优后表格（检索耗时、GC、传输体积），每项配一句"做了什么、为什么有效/无效"
- [ ] 15min：commit + push（含 gzip 配置和计时埋点），README 路线图勾 W6，写 W6 周报
- 📦 产出：**性能对比报告** = 8 周计划里唯一的"量化产出"，简历和博客都能引用

---

## W6 验收（周五晚）
| 验收项 | 标准 |
|---|---|
| 测量 | 首 token / 检索 / 总耗时三个计时在代码里 |
| 基线 | /api/hello 的 wrk 数字 + 一句"chat 为什么不压" |
| 优化实证 | 检索或传输体积至少一处前后对比数字 |
| 报告 | docs/performance.md 入库 |

## 打卡规则（沿用）
- ★完成 = 当日打卡；≥5天/周全勤；周日22:00周报硬指标
- **骨头日：Day 1 和 Day 3**。埋点是调优的前提，压测基线是报告的素材，都不能丢
- 豁免权照用，但 Day 7 的报告必须在本周内产出——它是 W8 博客的弹药

## 老周预警
1. W6 是"数字周"——本周所有结论都要有数字撑着，"感觉变快了"不算数
2. wrk 压 streaming 接口会挂很多长连接，数字会难看——这不是故障，是教学素材
3. JVM 那天的输出看不懂很正常，**抄下来 + 一句话猜测**就算过关，深挖归 W8 债