# 母题实验室 (Motif Lab)

用**母题故事动画**把难概念演明白：故事在讲，原理在演。孩子和成人共用一套极简界面。

- 设计：`docs/superpowers/specs/2026-07-12-motif-lab-design.md`
- 计划：`docs/superpowers/plans/2026-07-12-motif-lab-v0.1.md`
- 相关：教法参考 `E:\work\learn-skill`；壳能力参考 `E:\work\tepeu`（本项目独立，端口不同）

## 本地运行

端口 **30142**（避开 Tepeu 的 30141）。

### 一键（已构建静态资源）

```bash
cd backend
mvn spring-boot:run
```

浏览器打开：http://127.0.0.1:30142  
点「循环」→ 右侧播金牌动画 →「不懂/更简单」升简版 → 答题。

### 前端开发模式

```bash
cd frontend
npm install
npm run dev
```

`/api` 代理到 `http://127.0.0.1:30142`。

### 重新构建前端

```bash
cd frontend
npm run build
```

产物写入 `backend/src/main/resources/static/`。

## 配置 LLM（可选）

金牌「循环」**不需要** API Key。  
其它概念现生成动画：打开页面「设置」，填写兼容 OpenAI 的 `baseUrl` / `model` / `apiKey` 并启用。  
密钥加密保存在 `~/.motif-lab/provider.json`；主密钥 `~/.motif-lab/master.key`。

## v0.1 已验收

| 标准 | 结果 |
|------|------|
| 输入循环后迅速出现分镜 | 通过（本地 storyboard） |
| 缓存/金牌动画可播 | 通过（HTML 含 motif-lab gold） |
| 更简单升 L 级 | 通过（L0→L1） |
| 检验题通过 | 通过（phase=DONE） |
| 单元测试 | `mvn test` 通过 |

## 项目结构

```
motif-lab/
├── backend/     # Spring Boot
├── frontend/    # Vite + React
├── docs/        # 设计与计划
└── CONTEXT.md
```
