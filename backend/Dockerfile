# ============================================================
# 青禾商城 后端镜像（M3-9 容器化）
# 多阶段构建：maven 构建 → JRE 运行
# P2-19 加固：
#   - 非 root 运行（专用 app 用户，UID 1001）
#   - HEALTHCHECK 探测 /actuator/health（spring-boot-starter-actuator）
#   - 容器感知 JVM 参数（MaxRAMPercentage 按 cgroup 限额取堆）
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 先拷贝 pom 单独下载依赖，充分利用构建缓存
# P2-19：去掉 || true，依赖解析失败立即暴露（不再吞错）
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
# curl 仅用于 HEALTHCHECK，装完清理 apt 缓存控制镜像体积
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/qinghe-mall-1.0.0.jar app.jar
# 上传目录（M3-4 图片本地化），归属非 root 用户
RUN mkdir -p /app/upload \
    && groupadd -r app && useradd -r -g app -u 1001 app \
    && chown -R app:app /app
USER app
VOLUME ["/app/upload"]
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER__PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
# 数据库/Redis 口令通过环境变量注入（见 docker-compose.yml）
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=8080"]
# P2-19：容器健康检查（Spring Boot 启动较慢，放宽 start-period）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"UP"' || exit 1
