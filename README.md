# 母题实验室 (Motif Lab)

学习应用：通过母题故事动画帮助理解抽象概念。后端 Spring Boot + 前端 React/Vite，端口 **30142**。

## 本地运行

### 后端

```bash
cd backend
mvn spring-boot:run
```

### 前端（开发模式）

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器会将 `/api` 代理到 `http://127.0.0.1:30142`。

### 生产构建

```bash
cd frontend
npm run build
```

构建产物输出到 `backend/src/main/resources/static`，由后端统一提供静态资源。

## 项目结构

```
motif-lab/
├── backend/          # Spring Boot 后端
├── frontend/         # Vite + React 前端
├── docs/             # 设计与计划文档
└── CONTEXT.md        # 项目进度快照
```
