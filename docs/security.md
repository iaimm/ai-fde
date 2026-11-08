# NotesGPT 安全加固报告（W9）

&gt; 原则：单用户作品集，不堆砌功能，只加固"会被面试官问"的防线

## 已落地（四项）

### 1. 限流 — Bucket4j 令牌桶
- 30 次/分钟/IP，超限 429（ResponseEntity 显式 contentType——SSE produces 方法的坑）
- 选型理由：令牌桶允许短时突发（演示连点），同时守住长线；固定窗口计数器会边界清零
- 单机体感：内存桶，进程重启清零；集群需 Redis 令牌桶（面试标准答案，未实施）

### 2. 输入边界校验
- 空/空白 → 400；超 500 字 → 400
- **校验先于限流**——非法输入不消耗令牌，防"限流旁路"（垃圾请求耗光配额）
- ChatControllerTest 三用例（@MockitoBean，Boot 4 已移除 @MockBean）

### 3. XSS 防护 — 输出编码层定位
- 结论：输出编码层在 Vue 模板插值（{{ }} 默认转义），无 v-html
- **决策记录：后端刻意不做 HTML escape**——传输层转义会导致渲染层双重转义（&lt; 字面显示）
- 验证：xss-test 测试桩注入 &lt;script&gt;alert(1)&lt;/script&gt;，前端纯文本无弹窗
- 教训：安全加固先定位"输出编码层"，checklist 不能盲目照做

### 4. 结构化日志
- logback JSON 格式（PatternLayoutEncoder 手写，零额外依赖）
- [PERF] 日志从 System.out 迁移 SLF4J（绕过日志框架的输出不可观测）
- traceId 经 MDC 串联，finally 必 clear（ThreadLocal 残留污染 carrier 线程）

## 工程纪律（血泪版）
- **端口检查令**：任何 compose up 前，lsof :5173 :8080 必查（5173 已四访）
- **一端口一进程**：dev server 与容器映射同端口永远二选一
- **验证实体版本**：restart≠up；运行的代码/容器是哪个版本，先验证再假设
- 高基数铁律：监控指标标签值必须有限集合

## 非目标（有意不做）
JWT/认证授权（单用户）、HTTPS（本地部署）、CORS（vite proxy 开发期够用）