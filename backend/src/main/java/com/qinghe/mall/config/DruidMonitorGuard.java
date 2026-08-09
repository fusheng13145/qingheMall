package com.qinghe.mall.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Druid 监控台安全守卫（P0-7）。
 *
 * 背景：Druid stat-view-servlet 若以弱口令开启，SQL 监控台会暴露数据源
 * 连接信息、SQL 语句与表结构。base 配置已默认关闭（DRUID_MONITOR_ENABLED=false），
 * 本守卫作为纵深防御：任何环境显式开启监控台时，若口令为默认值/占位符/过短，
 * 直接拒绝应用启动，防止生产漏配强口令。
 */
@Component
public class DruidMonitorGuard {

    private static final Logger log = LoggerFactory.getLogger(DruidMonitorGuard.class);

    @Value("${spring.datasource.druid.stat-view-servlet.enabled:false}")
    private boolean monitorEnabled;

    @Value("${spring.datasource.druid.stat-view-servlet.login-username:druid}")
    private String loginUsername;

    @Value("${spring.datasource.druid.stat-view-servlet.login-password:druid}")
    private String loginPassword;

    /** 弱口令黑名单：默认值与 .env.example 占位符 */
    private static final String[] WEAK_PASSWORDS = {"druid", "change_me_strong", "123456", "admin", "password"};

    @PostConstruct
    public void guard() {
        if (!monitorEnabled) {
            return;
        }
        String pwd = loginPassword == null ? "" : loginPassword.trim();
        boolean weak = pwd.length() < 8;
        for (String weakPwd : WEAK_PASSWORDS) {
            if (pwd.equalsIgnoreCase(weakPwd)) {
                weak = true;
                break;
            }
        }
        if (weak) {
            throw new IllegalStateException(
                    "Druid 监控台已开启（DRUID_MONITOR_ENABLED=true）但登录口令过弱，禁止启动。"
                            + "请设置 ≥8 位强口令（DRUID_LOGIN_PASSWORD），或关闭监控台（DRUID_MONITOR_ENABLED=false）。");
        }
        log.warn("Druid 监控台已开启（仅建议内网/临时调试使用），用户={}", loginUsername);
    }
}
