# W10 打卡清单：可观测性周（90 天 Phase 1 Week 2）
&gt; 周期：Day 1（六）→ Day 7（五），Day 2（日）复盘日。每天 30 分钟。
&gt; 主题：给 NotesGPT 装上"眼睛"——指标采集（Actuator）→ 存储（Prometheus）→ 展示（Grafana）。
&gt; W9 欠账（D5 安全清单 / D7 security.md）并入本周 Day 2 清算。

## 预期管理
W9 的 JSON 日志解决"出了事能查"，W10 的 Metrics 解决"正在发生的事能看"。
面试话术锚点："我的服务有健康指标看板，QPS/延迟/JVM 一目了然"——这就是 50w 和 30w 工程师的分水岭之一。

---

## Day 1 ·（六）Actuator 指标端点（骨头日）
- [ ] ★ 15min：pom.xml 加依赖（走 Aliyun 镜像，下载应顺利；这次下载的都是小 jar）
  ```xml
  &lt;dependency&gt;
      &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
      &lt;artifactId&gt;spring-boot-starter-actuator&lt;/artifactId&gt;
  &lt;/dependency&gt;
  &lt;dependency&gt;
      &lt;groupId&gt;io.micrometer&lt;/groupId&gt;
      &lt;artifactId&gt;micrometer-registry-prometheus&lt;/artifactId&gt;
  &lt;/dependency&gt;
  ```
- [ ] 15min：application.properties 配置暴露端点 + 应用标签：
  ```properties
  management.endpoints.web.exposure.include=health,prometheus,metrics
  management.metrics.tags.application=notes-gpt
  ```
  验证：`curl localhost:8080/actuator/health` 返回 UP；`curl localhost:8080/actuator/prometheus` 返回大段 `# HELP` 指标文本
- 📦 产出：指标端点可用
- 🆘 降级：只验证 /actuator/health，prometheus 端点留 Day 3 一起验

## Day 2 ·（日）复盘日：W9 欠账清算（轻量）
- [ ] ★ 15min：写 `docs/security.md`（W9-D7 欠账）——素材全在：限流（Bucket4j 令牌桶 30/min/IP）、输入校验（先于限流防旁路）、XSS 决策记录（Vue 插值=输出编码层，后端 escape 属伪防御）、JSON 日志 + traceId。每项记"做了什么+为什么"
- [ ] 15min：Boot 4 文档统一（README + 简历 + security.md 里 "Spring Boot 3" 全部改 "Spring Boot 4"——"Boot 4 + Java 21 + 虚拟线程"是更亮的卖点）+ W9 周报
- 🆘 降级：只写 security.md，文档统一顺延

## Day 3 ·（一）Prometheus 上线（骨头日）
- [ ] ★ 15min：项目根目录新建 `prometheus.yml` + compose 加 service：
  ```yaml
  # prometheus.yml
  scrape_configs:
    - job_name: 'notes-gpt'
      metrics_path: '/actuator/prometheus'
      scrape_interval: 5s
      static_configs:
        - targets: ['backend:8080']     # compose 内网，用 service 名
  ```
  ```yaml
  # docker-compose.yml 追加
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ```
- [ ] 15min：`docker compose up -d prometheus` → 浏览器 localhost:9090 → Status → Targets → notes-gpt 显示 **UP** = 打通
- 📦 产出：Prometheus 持续抓取后端指标
- 🆘 降级：Targets UP 就算过，UI 查询玩不转不恋战

## Day 4 ·（二）Grafana 看板
- [ ] ★ 15min：compose 加 grafana（image: grafana/grafana, ports 3000:3000）→ 起容器 → 浏览器 localhost:3000（首次登录 admin/admin 改密跳过）→ Connections → Add data source → Prometheus → URL 填 `http://prometheus:9090` → Save & test 绿
- [ ] 15min：Dashboards → New → Import → 输 **4701**（JVM Micrometer 官方面板）→ Load → 选刚建的数据源。浏览器问个问题/压测 10 秒，看 JVM 曲线跳动 = 活面板
- 📦 产出：可视化 JVM 看板（截图！进 observability 报告）
- 🆘 降级：数据源配好 + 手动 Browse 里查 `jvm_memory_used_bytes` 一条曲线，不导面板

## Day 5 ·（三）业务自定义指标（骨头日）
- [ ] ★ 15min：ChatController 注入 MeterRegistry，加两个业务指标：
  ```java
  private final Counter questionCounter;
  private final Timer ttftTimer;
  // 构造器里：
  this.questionCounter = registry.counter("notesgpt.questions.total");
  this.ttftTimer = registry.timer("notesgpt.ttft");
  // forwardToLlm 里：questionCounter.increment();  拿到 ttftMs 后：ttftTimer.record(Duration.ofMillis(ttftMs));
  ```
- [ ] 15min：重启问 3 个问题 → Prometheus UI 搜 `notesgpt_questions_total` 应为 3；`notesgpt_ttft_count` 同步增长
- 📦 产出：业务指标入监控体系
- 🆘 降级：只加计数器，Timer 跳过

## Day 6 ·（四）告警初识 + 全栈验收
- [ ] ★ 15min：prometheus.yml 顶层加 alerting 规则（先理解三要素：条件/持续时间/动作，单机演示动作只有页面显示）：
  ```yaml
  rule_files:
    - "alerts.yml"
  # alerts.yml 同级新建：
  groups:
    - name: notes-gpt
      rules:
        - alert: BackendDown
          expr: up{job="notes-gpt"} == 0
          for: 1m
          annotations:
            summary: "后端挂了"
  ```
  `docker compose restart prometheus` → Alerts 页看到 BackendDown（绿色 inactive 状态，正常）
- [ ] 15min：全栈验收：`docker compose up -d` 四服务（backend/frontend/prometheus/grafana）→ 5173 问答 → 3000 看板 → 9090 targets 全 UP → **截图四张存档**（W10 报告素材）
- 🆘 降级：只配规则不验收，截图归 Day 7

## Day 7 ·（五）可观测性报告日（W10 验收）
- [ ] ★ 15min：写 `docs/observability.md`：架构一段话（App 暴露 /actuator/prometheus ← Prometheus 5s 抓取 ← Grafana 展示）+ 四张截图 + 指标清单表（JVM 内置 / 业务自定义 / 健康检查）
- [ ] 15min：commit + push（prometheus.yml、compose、alerts.yml、埋点代码、两份 docs）+ W10 周报
- 📦 产出：**可观测性报告**——和 security.md、performance.md 并列成作品集三件套

---

## W10 验收标准（Day 7 逐条过）
| 验收项 | 标准 |
|---|---|
| 指标端点 | /actuator/prometheus 输出指标文本 |
| 抓取 | Prometheus Targets 显示 notes-gpt UP |
| 看板 | Grafana 导入 4701 面板，曲线随压测跳动 |
| 业务指标 | notesgpt_questions_total 随问答增长 |
| 告警 | Alerts 页可见 BackendDown 规则（inactive） |
| 报告 | observability.md 入库，三报告并列 |

## 打卡规则（沿用）
- ★完成 = 当日打卡；≥5 天/周全勤；周日 22:00 周报
- **骨头日：Day 1（指标端点）和 Day 3（Prometheus 打通）**。端点是地基，打通是分水岭，都不能丢
- 豁免权照用，但 Day 7 报告必须在本周产出

## 老周预警
1. **Actuator 暴露纪律**：只暴露 health/prometheus/metrics 三个端点。**绝不要暴露 env、heapdump、threaddump**——heapdump 泄露等于把内存里的 API_KEY 送给攻击者。这是安全面试的标准考点，security.md 补一句
2. **指标标签禁忌**：自定义指标的标签（tag）值必须是**有限集合**（如 status=ok/fail）。**绝对不能用 question 文本当标签**——每个问题一个新时间序列，几千条问题就能撑爆 Prometheus（高基数灾难）。50w 面试的经典追问点
3. **端口抢占老坑**：起 prometheus/grafana 前先 `lsof -i :9090` 和 `lsof -i :3000`——和当年 5173 被 npm dev 抢占是同一族坑
4. **WSL 内存**：Prometheus+Grafana 两个容器常驻约 500MB 内存，WSL 卡的话 Day 6 验收完 `docker compose stop prometheus grafana`，需要时再起