## XSS 防护（W9-D4）
- 输出编码层：Vue 模板插值 {{ }}（默认 HTML 转义），前端无 v-html/innerHTML（grep 验证）
- 后端刻意不做 HTML escape：防止双重转义（&lt; 字面显示），且转义应发生在渲染层而非传输层
- 验证方式：xss-test 测试桩注入 <script>alert(1)</script>，前端显示纯文本无弹窗
- 教训：安全加固先定位"输出编码层"，checklist 不能盲目照做