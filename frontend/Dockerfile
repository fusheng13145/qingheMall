# ============================================================
# 青禾商城 前端镜像（M3-9 容器化）
# 多阶段构建：node 构建 → nginx 托管静态资源 + 反向代理 /api
# ============================================================
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
