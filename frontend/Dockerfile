# ============================================================
# 青禾商城 前端镜像（M3-9 容器化）
# 多阶段构建：node 构建 → nginx 托管静态资源 + 反向代理 /api
# P1-15/19：Node 18 EOL → 20 LTS；nginx 1.25 EOL → 1.27
# P1-3：新增可选 HTTPS（SSL_ENABLED=true + 挂载证书）
# P2-19：新增 HEALTHCHECK
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
# HTTP 默认配置（P0-2 已含 client_max_body_size / gzip / 缓存）
COPY nginx.conf /etc/nginx/conf.d/default.conf
# HTTPS 配置暂存，SSL_ENABLED=true 时由入口脚本切换启用
COPY nginx-ssl.conf /etc/nginx/ssl.conf
# SSL 切换入口脚本（nginx 官方镜像自动执行 /docker-entrypoint.d/）
COPY docker-entrypoint-ssl.sh /docker-entrypoint.d/40-ssl-select.sh
RUN chmod +x /docker-entrypoint.d/40-ssl-select.sh
EXPOSE 80 443
# P2-19：容器健康检查（探测 SPA 入口）
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1
