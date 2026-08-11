# HTTPS 证书目录（P1-3）

将以下两个文件放入本目录后，在 `.env` 设置 `SSL_ENABLED=true`、`COOKIE_SECURE=true`，
重新 `docker compose up -d --build` 即可启用 HTTPS（80 自动 301 跳转 443）。

| 文件 | 说明 |
| --- | --- |
| `fullchain.pem` | 服务器证书 + 中间证书链（PEM 拼接） |
| `privkey.pem` | 证书对应私钥（PEM） |

注意事项：

- 证书缺失而 `SSL_ENABLED=true` 时，前端容器将 **fail-closed 拒绝启动**（不会静默降级为明文）。
- 可用 Let's Encrypt 签发，例如：
  `certbot certonly --standalone -d your-domain.com`，
  再将 `/etc/letsencrypt/live/your-domain.com/fullchain.pem` 与 `privkey.pem` 复制到本目录。
- 本目录内容**不要提交入仓**（私钥敏感）；已在 `.gitignore` 忽略 `*.pem`/`*.key`。
