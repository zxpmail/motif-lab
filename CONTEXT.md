# CONTEXT.md
# 项目进度快照（极简）

## 当前在做什么
修复 Provider：误填 Tepeu 的 DeepSeek `/anthropic` 地址会导致 LLM 404（模型看似不生效）。

## 上次停在哪
https://github.com/zxpmail/motif-lab · 本地 http://127.0.0.1:30142

## 近期关键决定
- Motif Lab 只用 OpenAI 兼容 `/chat/completions`；DeepSeek baseUrl = `https://api.deepseek.com/v1`
- 保存/调用时自动把 `.../anthropic` 改写成 `/v1`
- 金牌课（循环/变量/条件/函数）不走模型
