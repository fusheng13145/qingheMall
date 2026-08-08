# ============================================================
# 青禾商城 后端镜像（M3-9 容器化）
# 多阶段构建：maven 构建 → JRE 运行
# ============================================================
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /app
# 先拷贝 pom 单独下载依赖，充分利用构建缓存
COPY pom.xml .
RUN mvn -q dependency:go-offline || true
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/target/qinghe-mall-1.0.0.jar app.jar
# 上传目录（M3-4 图片本地化）
RUN mkdir -p /app/upload
VOLUME ["/app/upload"]
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER__PORT=8080
# 数据库/Redis 口令通过环境变量注入（见 docker-compose.yml）
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]
