package com.travelmate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class DatabaseStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupValidator.class);

    private final DataSource dataSource;
    private final Environment environment;

    public DatabaseStartupValidator(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");

        try (Connection connection = dataSource.getConnection()) {
            log.info("Database connection verified: {}", connection.getMetaData().getURL());
        } catch (CannotGetJdbcConnectionException e) {
            logConnectionFailure(url, username, password, e);
            throw new IllegalStateException("数据库连接失败，请检查 backend/application-local.yml 或 DB_PASSWORD 配置", e);
        } catch (Exception e) {
            if (contains(e.getMessage(), "Access denied for user")
                    || contains(e.getMessage(), "Communications link failure")) {
                logConnectionFailure(url, username, password, e);
                throw new IllegalStateException("数据库连接失败，请检查 backend/application-local.yml 或 DB_PASSWORD 配置", e);
            }
            throw e;
        }
    }

    private void logConnectionFailure(String url, String username, String password, Exception exception) {
        log.error(
                "Database connection validation failed. Check backend/application-local.yml or DB_PASSWORD. url={}, username={}, passwordLength={}",
                url,
                username,
                password == null ? 0 : password.length(),
                exception);

        if (url == null || username == null || password == null) {
            log.error("Skipping direct DriverManager probe because datasource properties are incomplete.");
            return;
        }

        try (Connection ignored = DriverManager.getConnection(url, username, password)) {
            log.info("Direct DriverManager probe succeeded with runtime datasource properties.");
        } catch (SQLException probeException) {
            log.error(
                    "Direct DriverManager probe failed with runtime datasource properties. url={}, username={}, passwordLength={}",
                    url,
                    username,
                    password.length(),
                    probeException);
        }
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
    }
}