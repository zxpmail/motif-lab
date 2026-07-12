# CONTEXT.md
# 项目进度快照（极简）

## 当前在做什么
修复非金牌课质量：空壳分镜 → LLM 教案包（真分镜+口诀+题）再生成动画。

## 上次停在哪
https://github.com/zxpmail/motif-lab · 本地 http://127.0.0.1:30142 · protocol v2

## 近期关键决定
- 工厂模式等非金牌概念必须先「笨办法 vs 聪明办法」对照，禁止只喊概念名
- 缓存键含 sceneSeed；换故事会重新生成
- DeepSeek baseUrl 用 `/v1`（OpenAI 兼容）
