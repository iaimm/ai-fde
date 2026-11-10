## 3. 演进剧本：从 1 千到 10 万 DAU

**核心纪律：每阶段只解决该阶段的真实瓶颈，不提前消费复杂度。**
（1 千 DAU 就上 K8s 不是架构能力强，是判断力弱。）

### 阶段一：1 千 DAU（现状，已落地）

单体 Spring Boot + nginx 前端，会话存浏览器 localStorage，笔记在磁盘。
无任何外部中间件，docker compose 四容器一把梭。
**此阶段没有瓶颈——没有问题就没有方案，照实写现状就是本节的内容。**

```mermaid
graph LR
    U[用户] --&gt; F[frontend&lt;br/&gt;nginx + dist]
    F --&gt;|/api 反代| B[backend 单体&lt;br/&gt;Spring Boot 4]
    B --&gt;|检索| N[(notes/*.md)]
    B --&gt;|SSE 转发| K[Kimi API]
```

### 阶段二：1 万 DAU——状态丢失 + 重复计算

**瓶颈怎么来的**：用户换设备会话没了（localStorage 绑浏览器）；热门问题被反复检索，同样的问题一天算几百次（1.2 QPS 不高，但全是重复功）。

| 方案 | Trade-off |
|---|---|
| Redis 缓存 top3 检索结果，TTL 5 分钟 | **Redis 而非 Guava Cache**：多实例共享命中，本地缓存每实例各算各的；**TTL 5 分钟**：新鲜度与命中率的折中——笔记更新后最坏 5 分钟生效，可接受；不写缓存穿透保护（本地作品，1 万 DAU 无恶意流量） |
| MySQL 存会话历史 | **MySQL 而非 PG**：团队熟、生态广，无 PG 特有需求；**后端持久化而非 localStorage**：跨设备的代价是引入登录态——这是刻意接受的复杂度增量，1 千 DAU 时绝对不做（YAGNI） |

```mermaid
graph LR
    U[用户] --&gt; F[frontend]
    F --&gt; B[backend 单体]
    B --&gt;|检索（先查缓存）| N[(notes/*.md)]
    B --&gt; R[(Redis&lt;br/&gt;top3 结果 5min)]
    B --&gt; M[(MySQL&lt;br/&gt;会话历史)]
    B --&gt; K[Kimi API]
```

### 阶段三：10 万 DAU——LLM 配额被打爆 + 检索拖垮主服务

**瓶颈怎么来的**：峰值 120 QPS 直打 LLM，撞 kimi 限流 + 成本爆炸（见 §4 成本算式）；
检索是无状态 CPU 密集逻辑，和问答转发混在一个进程里，扩容时内存被连带翻倍。

| 方案 | Trade-off |
|---|---|
| LLM 调用全部改走 MQ 排队，Worker 按配额匀速消费，结果 SSE 推回 | **RabbitMQ 而非 Kafka**：10 万 DAU 的队列吞吐远摸不到 Kafka 的主场，Rabbit 的路由和延迟更贴场景；**削峰的代价是延迟**：高峰时答案从"立即"变"排队 N 秒"——用队列深度指标+前端排队提示兜底，这是拿体验换配额安全 |
| 检索拆独立服务，2 核起步水平扩容 | **只拆检索**：无状态、纯 CPU、零改造代价，是最先该出单体的一块；问答转发暂不拆（它薄，扩整个 jar 也不算贵）；**拆分的代价是网络一跳**——15ms 的检索变 20ms，可接受 |
| 网关统一限流 + 鉴权 | 把 Bucket4j 单机桶收编到网关层（Redis 令牌桶），**终集群限流方案的落地点**；单机内存桶在阶段二多实例后就已失效（此句点题 W9 的伏笔） |

```mermaid
graph LR
    U[用户] --&gt; GW[网关&lt;br/&gt;限流/鉴权]
    GW --&gt; F[frontend ×N]
    GW --&gt; B[问答服务 ×N&lt;br/&gt;SSE 保持]
    B --&gt; RS[检索服务 ×N&lt;br/&gt;无状态 CPU 密集]
    RS --&gt; N[(notes/*.md)]
    RS --&gt; R[(Redis)]0-+0.
    * 
    B --&gt; M[(MySQL)]
    B --&gt;|排队| MQ[[RabbitMQ]]
    MQ --&gt; W[LLM Worker ×N&lt;br/&gt;按配额匀速消费]
    W --&gt; K[Kimi API]
    W --&gt;|结果推送| B
```

### 演进总览（一张表收束）

| 阶段 | DAU | 新增组件 | 驱动瓶颈 |
|---|---|---|---|
| 一 | 1 千 | — | 无 |
| 二 | 1 万 | Redis, MySQL | 状态丢失、重复计算 |
| 三 | 10 万 | MQ, 检索服务, 网关 | LLM 配额、CPU 混部 |

## Day 4 ·（二）架构文档 III：容量估算（数字篇）
- [ ] ★ 15min：写第四节容量估算表，**每个数字必须写出算式**（面试官会当场验算）：
  ```markdown
  ## 4. 容量估算（10万 DAU）
  | 项 | 算式 | 结果 |
  |---|---|---|
  | 峰值 QPS | 10万DAU × 10次/人 ÷ 86400s × 10倍峰值系数 | ≈116，按 120 设计 |
  | LLM 调用成本 | 100万次/天 × 平均3K tokens × 单价 | 月成本 ___ 元 |
  | Redis 内存 | 100 分块 × 1KB × 问题维度缓存 ×10 | &lt; 10 MB，单机足矣 |
  | 检索服务 CPU | 15ms/次 × 120 QPS = 1.8 核 | 2 核起步，按 80% 水位扩 |
  ```
  （LLM 单价去 Kimi 官网查当前价，填真实数字——可复算=可信）
- [ ] 15min：写第五节"明确不做"（Non-Goals）——**这节最能体现架构品味**：
  不上 K8s（Docker Compose + LB 足够）、不做向量检索（关键词够用，Embedding 是检索质量问题不是架构问题）、不分库分表（单表单库撑 10 万 DAU 绰绰有余）
- 📦 产出：数字表 + Non-Goals 定稿
- 🆘 降级：容量表只算 QPS 一行，其余 W12 补

## Day 5 ·（三）Alertmanager 通知闭环（骨头日）
- [ ] ★ 15min：compose 加 alertmanager（image: prom/alertmanager, ports 9093:9093），`alertmanager.yml`：route → receiver webhook → `http://backend:8080/api/webhook/alert`；prometheus.yml 顶层 `alerting: alertmanagers: [static_configs: [targets: ['alertmanager:9093']]]`
- [ ] 15min：ChatController 加 5 行调试接收端点（`@PostMapping("/api/webhook/alert")`，body 打日志——JSON 日志自动带 traceId）→ `up -d --build backend alertmanager` → 复用 firing 验证（stop backend 等 firing）→ `docker compose logs backend | grep alert` 看到告警送达记录 = **通知闭环**
- 📦 产出：告警链路从"看得见"升级到"送得到"
- ⚠️ 处置：该端点无鉴权，security.md 补一句"调试端点，生产需鉴权/IP 白名单"
- 🆘 降级：只装 Alertmanager 容器，webhook 端点跳过

## Day 6 ·（四）技术债清理日
- [ ] ★ 15min：pom 清理——删冗余 `spring-boot-starter-web`（留 webmvc）、删显式 jackson 版本（交给 Boot 管理）→ `mvn clean test` 全绿才敢 push；Grafana UI 删掉 "JVM (Micrometer)1/2" 两个舶来副本
- [ ] 15min：博客《一个 27KB 的 jar》开写：六层排错链 + 依赖树验 POM 不验 jar + "先验证运行的实体再假设"总心法。写完发掘金，文末挂仓库链接
- 📦 产出：pom 干净 + 博客上线（W8 欠账顺带清一笔）
- 🆘 降级：只清 pom，博客留 W12

## Day 7 ·（五）Phase 1 收官日
- [ ] ★ 15min：**简历追加两行**（项目业绩补）：
  &gt; · 可观测性建设：Micrometer + Prometheus + Grafana 指标链路，告警（Alertmanager）全链路 firing 验证；GC/TTFT/业务量双源测量互证
  &gt; · 产出系统设计文档（docs/architecture.md）：10万 DAU 三阶段演进 + 容量估算 + Trade-off 决策记录
- [ ] 15min：Phase 1 收官报告（30 天：4 项安全加固 + 观测三件套 + 告警闭环 + 架构文档 + 1 篇博客）+ W11 周报；Phase 2 预告（前端 TS 化）
- 📦 产出：Phase 1 正式闭幕，简历弹药再升级

---

## W11 验收标准（Day 7 逐条过）
| 验收项 | 标准 |
|---|---|
| 架构文档 | architecture.md 含 C4 现状图 + 三阶段演进 + 容量算式 + Non-Goals |
| Trade-off | 每个技术选择至少一行"为什么/放弃什么" |
| 告警闭环 | Alertmanager → backend 日志可见送达记录 |
| 技术债 | pom 无冗余依赖，mvn test 绿 |
| 博客 | 《27KB 的 jar》发布且链接回仓库 |
| 简历 | 新增可观测性 + 架构设计两条业绩 |

## 打卡规则（沿用）
- ★完成 = 当日打卡；≥5 天/周全勤；周日 22:00 周报
- **骨头日：Day 1（C4 图）、Day 3（演进剧本）、Day 5（告警闭环）**
- 豁免权照用，但 architecture.md 和简历更新必须在本周内产出

## 老周预警
1. 架构文档第一大忌：技术清单化——每行方案必须回答"为什么不是别的"
2. 容量数字必须可复算：写出算式，允许面试官拿计算器验
3. 演进剧本别一步到位：分阶段的魅力=每阶段只解决当时瓶颈
4. Non-Goals 是加分王牌：敢写"不做什么、为什么不"的候选人极少