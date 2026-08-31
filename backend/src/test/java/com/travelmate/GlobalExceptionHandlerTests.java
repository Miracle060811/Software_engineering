package com.travelmate;

import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void runtimeExceptionReturnsItsRootCauseMessage() {
        Result<?> result = handler.handleRuntimeException(
                new RuntimeException("outer", new IllegalStateException("具体失败原因")));

        assertError(result, "具体失败原因");
    }

    @Test
    void checkedExceptionWithoutMessageUsesSafeFallback() {
        Result<?> result = handler.handleException(new Exception());

        assertError(result, "操作失败");
    }

    @Test
    void jdbcConnectionFailureReturnsConfigurationGuidance() {
        CannotGetJdbcConnectionException exception = new CannotGetJdbcConnectionException(
                "connection failed", new SQLException("server unavailable"));

        Result<?> result = handler.handleRuntimeException(exception);

        assertError(result, "数据库连接失败，请检查 backend/application-local.yml 或 DB_PASSWORD 配置");
    }

    @Test
    void nestedConnectionFailureMessageReturnsConfigurationGuidance() {
        Result<?> result = handler.handleRuntimeException(
                new RuntimeException("outer", new IllegalStateException("Communications link failure")));

        assertError(result, "数据库连接失败，请检查 backend/application-local.yml 或 DB_PASSWORD 配置");
    }

    @Test
    void missingTableMessageReturnsInitializationGuidance() {
        Result<?> result = handler.handleRuntimeException(
                new RuntimeException("query failed", new SQLException("Table 'travelmate.tm_user' doesn't exist")));

        assertError(result, "数据库表不存在，请先执行 docs/sql/init.sql 初始化数据库");
    }

    @Test
    void responseStatusExceptionPreservesHttpStatus() {
        ResponseEntity<Result<?>> response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertError(response.getBody(), "内部服务凭证无效");
    }

    private void assertError(Result<?> result, String expectedMessage) {
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).isEqualTo(expectedMessage);
        assertThat(result.getData()).isNull();
    }
}
