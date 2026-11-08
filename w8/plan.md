# W7 打卡清单：整合打磨周（作品集周）
&gt; 周期：Day 1（六）→ Day 7（五），Day 2（日）复盘日。每天 30 分钟。
&gt; 本周原则：不加新功能，只打磨"别人第一眼看到的东西"。

## Day 1 ·（六）F5 引用溯源：作品集最大"哇塞点"（骨头日）
- [ ] ★ 15min：后端——流结束前发一帧引用数据：`emitter.send(SseEmitter.event().name("refs").data(JSON字符串))`，JSON 里是 top3 的 [{src, 原文}]
- [ ] 15min：前端——识别 `event: refs` 帧，回答下方渲染成可点击的来源 chips，点击弹出/展开原文段落
- 📦 产出：点击 [1] 能看到笔记原文
- 🆘 降级：来源只显示文件名列表（不弹原文），也算 F5 简化版过

## Day 2 ·（日）复盘日（轻量）
- [ ] ★ 15min：缺口清单更新 + W6 周报（素材：性能报告 + wrk 假数字案例）
- [ ] 15min：22:00 前发周报
- 🆘 降级：只发周报

## Day 3 ·（一）README 大改造（骨头日）
- [ ] ★ 15min：后端 README 重写：一句话价值 → 架构 mermaid 图 → 接口表 → 性能数据节选（引用 W6 报告数字）→ 双启动方式 → 路线图全勾
- [ ] 15min：前端 README 重写：截图换最新（带引用溯源功能）、功能清单、动图占位（Day 4 填）
- 📦 产出：两个 README 达到"陌生人 3 分钟看懂并想跑"的标准
- 🆘 降级：只改后端 README（它是主叙事）

## Day 4 ·（二）录屏素材日
- [ ] ★ 15min：录一段 60 秒内演示（提问→流式→点引用看原文→刷新会话还在），Windows 按 Win+G 或装 ScreenToGif；存 frontend/docs/demo.gif
- [ ] 15min：gif 压到 5MB 以内（ScreenToGif 自带压缩/ezygif.com 在线压），嵌进前端 README
- 📦 产出：README 里会动的 demo
- 🆘 降级：录屏存 mp4 放仓库，README 放链接

## Day 5 ·（三）仓库门面日
- [ ] ★ 15min：双仓库各加 LICENSE（MIT：GitHub 网页 Add file → Create new file → 输 LICENSE 选 MIT）
- [ ] 15min：双仓库 Settings 补全：About 描述一句话、Topics 标签（spring-boot, rag, vue3, sse, docker, llm）、勾上 website 链接
- 📦 产出：仓库页从"毛坯"变"精装"
- 🆘 降级：只加 LICENSE

## Day 6 ·（四）作品集汇总页
- [ ] ★ 15min：建 GitHub 个人主页仓库（`你的用户名/你的用户名`，special repo），README 放：自我介绍 3 行 + NotesGPT 项目卡（截图+链接+三关键词：全栈/RAG/流式）
- [ ] 15min：把 ai-fde 笔记仓也顺手收拾：README 说明这是学习笔记库，挂 badge
- 📦 产出：点你 GitHub 头像就有作品展示
- 🆘 降级：只做 special repo

## Day 7 ·（五）最终验收 + W8 弹药准备
- [ ] ★ 15min：全流程过一遍验收清单（见下表），截图存档
- [ ] 15min：列 W8 三个博客选题 + 各写 5 行大纲（推荐：《SSE 流式踩坑实录》《我的一次配置漂移事故》《RAG 诚实性设计：让模型说"不知道"》）
- 📦 产出：作品集定稿 + W8 选题表

## W7 验收清单（Day 7 逐条过）
- [ ] 陌生人克隆仓库，按 README 5 分钟跑起来（Docker 方式）
- [ ] 回答带 [编号]，点击能看到出处原文
- [ ] 刷新页面会话还在
- [ ] README 有架构图 + 性能数字 + 动图
- [ ] 双仓库有 LICENSE、Topics、描述
- [ ] GitHub 个人主页能看到项目卡

## 打卡规则（沿用）
- ★完成 = 当日打卡；≥5天/周全勤；周日22:00周报
- 骨头日：Day 1（F5）和 Day 3（README）。F5 是技术亮点，README 是门面，丢一个本周就白过
- 豁免权照用，但 Day 7 验收清单必须全勾才算 W7 过

## 老周预警
1. 本周是"展示面"周——你写每一行 README 时，假想读者是"只给你 3 分钟的面试官"
2. F5 的后端 refs 帧是 SSE 的 `event:` 字段用法，前端按事件名解析——Day 1 前可以先问 AI "SSE 自定义事件名怎么发怎么收"，带着答案动手
3. 录屏别追求完美，一镜到底有瑕疵反而真实