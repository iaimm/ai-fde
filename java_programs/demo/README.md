# NotesGPT Backend

&gt; 把你的技术笔记变成可问答的 AI 知识库 —— Spring Boot 3 + SSE 流式 + RAG 检索

![CI](https://github.com/iaimm/notes-gpt-backend/actions/workflows/ci.yml/badge.svg)

## 功能
- SSE 流式问答，逐字输出
- 关键词检索 RAG（top-3 分块，答案带 [编号] 出处）
- 引用溯源：流末尾 `refs` 帧携带原文，前端点击可展开
- 笔记外问题诚实回答"笔记中无法确定"（非编造）

## 架构

​```mermaid
graph LR
    A[Vue3 前端] --&gt;|GET /api/chat/stream| B[Spring Boot 3]
    B --&gt; C[NoteRetriever&lt;br/&gt;分块 + 关键词打分]
    B --&gt;|HttpClient 转发| D[kimi API]
    C -. 启动时加载 .-&gt; E[(notes/*.md)]
​```

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/chat | 同步问答（调试） |
| GET  | /api/chat/stream?question= | SSE 流式（末尾 event: refs 帧） |

## 性能（节选，全文 docs/performance.md）
- wrk 基线：147,848 RPS / 0.78ms（/test/api/hello，纯应用端点）
- 问答链路：检索 &lt;15ms；TTFT ~12s 由模型侧主导；应用代码是纯管道
- GC：112 次停顿共 203ms（30s 压测，0.68%）

## 启动

本地：JDK 21 + `export MOONSHOT_API_KEY=sk-xxx` → `mvn spring-boot:run`
Docker：`docker compose up`（见上级目录 compose.yml，前端配套）

## 路线图
- [x] W3 同步问答　[x] W4 SSE+RAG　[x] W5 Docker/CI　[x] W6 性能报告　[x] W7 引用溯源
- 非目标：多用户/权限、云端部署（单用户作品集设计）