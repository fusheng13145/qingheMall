#!/bin/sh
# ============================================================
# 青禾商城 前端 nginx —— SSL 配置切换（P1-3）
# nginx 官方镜像会在启动前自动执行 /docker-entrypoint.d/ 下的脚本。
# 行为：
#   SSL_ENABLED 未设置或非 true  -> 保持 HTTP 配置（default.conf），行为不变。
#   SSL_ENABLED=true 且证书齐全 -> 用 nginx-ssl.conf 覆盖 default.conf，启用 443 + 301 跳转。
#   SSL_ENABLED=true 但证书缺失 -> 直接报错退出（fail-closed，避免静默降级为明文）。
# ============================================================
set -e

CERT=/etc/nginx/ssl/fullchain.pem
KEY=/etc/nginx/ssl/privkey.pem
SSL_CONF=/etc/nginx/ssl.conf

if [ "$SSL_ENABLED" = "true" ]; then
    if [ -f "$CERT" ] && [ -f "$KEY" ]; then
        cp "$SSL_CONF" /etc/nginx/conf.d/default.conf
        echo "[qinghe-entrypoint] SSL enabled: HTTPS on 443, HTTP->HTTPS redirect."
    else
        echo "[qinghe-entrypoint] ERROR: SSL_ENABLED=true but certs missing ($CERT / $KEY). Refusing to start in plaintext." >&2
        exit 1
    fi
else
    echo "[qinghe-entrypoint] SSL disabled: serving HTTP only (set SSL_ENABLED=true + mount certs to enable HTTPS)."
fi
