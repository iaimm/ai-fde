# NotesGPT 可观测性报告（W10）

&gt; 日期：2026-11　栈：Spring Boot 4.1.2 + Micrometer + Prometheus + Grafana（compose 四服务）
&gt; 三报告体系：performance.md（W6 测量）· security.md（W9 安全）· 本文（W10 观测）

## 1. 架构与链路

应用 /actuator/prometheus（Micrometer 自动埋点 + 业务自定义）
← Prometheus 每 5s 抓取（job=notes-gpt，target=backend:8080）
← Grafana 可视化（手搓面板）+ 告警规则评估（alerts.yml）

compose 拓扑：backend :8080 · frontend :5173 · prometheus :9090 · grafana :3000

## 2. 指标体系

| 类别 | 指标 | 说明 |
|---|---|---|
| JVM 内置 | jvm_memory_used/committed/max_bytes | 堆内存；GC 锯齿可视化 |
| JVM 内置 | jvm_threads_live_threads | 线程数 |
| 业务自定义 | notesgpt_questions_total | 问答计数——计数点在 forwardToLlm（校验+限流之后），天然只计合法请求 |
| 业务自定义 | notesgpt_ttft_seconds_*（count/sum/max） | TTFT 计时，Timer 自动暴露三值 |
| 健康 | /actuator/health、up | 存活探针 + 告警数据源 |

## 3. 关键发现（数据说话）

- **堆惰性分配**：used 20.5MiB / committed 80MiB / max 7.83GiB——Xmx 是上限不是预占
- **GC 从数字变成形状**：W6 数出 112 次停顿（grep gc.log）→ W10 面板直接看到 Minor GC 锯齿
- **双源互证**：监控 ttft_seconds_max ≈ 13s 与 W6 手工埋点 13.7s 互相印证
- **计数精确性**：2 次提问 = counter 精确为 2

## 4. 告警

- BackendDown：`up{job="notes-gpt"} == 0`，for: 1m，severity=critical
- 三要素：expr（条件）/ for（持续时间）/ annotations（内容）
- 活体验证：stop backend → pending → **firing（red，截图）** → start → inactive
- 边界说明：单机动作只有页面显示；生产需 Alertmanager → 邮件/钉钉（面试话术）

## 5. 排障实录（八连，方法论资产）

| # | 现象 | 根因 | 心法 |
|---|---|---|---|
| 1 | 面板不动 | 宿主机 mvn 与容器抢 8080，Prometheus 抓空气 | 监控验收必须全栈容器化 |
| 2 | 容器起不来 not a directory | bind mount 源不存在→Docker 自动造毒目录 | 源文件先建，再重启 |
| 3 | 规则不生效 | restart 复用旧容器，新挂载不生效 | up 才重建，restart 只重启 |
| 4 | 误以为规则加载 | "Starting rule manager"零规则也打这行 | 日志措辞≠事实，要验证 |
| 5 | grep 验配置为空 | 编辑器缓冲≠磁盘 | grep 验磁盘是唯一真相 |
| 6 | alerts.yml 在容器里但规则空 | rule_files 没引用=文件白挂 | 挂载了≠被引用 |
| 7 | 4701 导入面板无数据 | 社区老面板指标命名偏差 | 舶来品用完即弃，手搓更快 |
| 8 | used 曲线看着平 | max 序列撑爆 Y 轴压扁真实波动 | 查摆非业务序列 |

## 6. 截图存档
① GC 锯齿面板 ② 手搓 JVM 面板（去 max 后） ③ 业务计数阶梯线 ④ Alerts green ⑤ firing red