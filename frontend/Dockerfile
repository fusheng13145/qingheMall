# ============================================================
# 青禾商城 前端镜像（M3-9 容器化）
# 多阶段构建：node 构建 → nginx 托管静态资源 + 反向代理 /api
# P1-15/19：Node 18 EOL → 20 LTS；nginx 1.25 EOL → 1.27
# ============================================================
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
# npm ci 依据 package-lock.json 精确安装，保证镜像构建与本地/CI 依赖一致
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
