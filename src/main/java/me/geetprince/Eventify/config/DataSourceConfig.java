package me.geetprince.Eventify.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/eventify}")
    private String configuredUrl;

    @Value("${spring.datasource.username:}")
    private String configuredUsername;

    @Value("${spring.datasource.password:}")
    private String configuredPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:5}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:1}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Bean
    @Primary
    public DataSource dataSource() {
        // Priority 1: Check DATABASE_URL (Render default injected env var)
        String envDbUrl = System.getenv("DATABASE_URL");
        String effectiveUrl = (envDbUrl != null && !envDbUrl.isBlank()) ? envDbUrl.trim() : configuredUrl.trim();

        String username = configuredUsername;
        String password = configuredPassword;

        // Normalize postgres:// or postgresql:// URLs (commonly supplied by Render / Heroku) to jdbc:postgresql://
        if (effectiveUrl.startsWith("postgres://") || effectiveUrl.startsWith("postgresql://")) {
            try {
                URI uri = new URI(effectiveUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                }
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                effectiveUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
                if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                    effectiveUrl += "?" + uri.getQuery();
                }
                log.info("Normalized Render postgres:// URL to JDBC format: {}", effectiveUrl);
            } catch (Exception e) {
                log.warn("Could not parse URI from URL '{}', using prefix substitution: {}", effectiveUrl, e.getMessage());
                if (effectiveUrl.startsWith("postgres://")) {
                    effectiveUrl = "jdbc:postgresql://" + effectiveUrl.substring("postgres://".length());
                } else if (effectiveUrl.startsWith("postgresql://")) {
                    effectiveUrl = "jdbc:postgresql://" + effectiveUrl.substring("postgresql://".length());
                }
            }
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(effectiveUrl);
        if (username != null && !username.isBlank()) {
            hikariConfig.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            hikariConfig.setPassword(password);
        }
        if (driverClassName != null && !driverClassName.isBlank()) {
            hikariConfig.setDriverClassName(driverClassName);
        }

        hikariConfig.setMaximumPoolSize(maximumPoolSize);
        hikariConfig.setMinimumIdle(minimumIdle);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);

        return new HikariDataSource(hikariConfig);
    }
}
