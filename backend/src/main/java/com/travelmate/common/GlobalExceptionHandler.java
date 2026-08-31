package com.travelmate.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<?>> handleResponseStatusException(ResponseStatusException e) {
        String message = e.getReason() == null || e.getReason().isBlank()
                ? "请求处理失败"
                : e.getReason();
        return ResponseEntity.status(e.getStatusCode()).body(Result.error(message));
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("Unhandled runtime exception", e);
        return Result.error(resolveMessage(e));
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return Result.error(resolveMessage(e));
    }

    private String resolveMessage(Throwable throwable) {
        Throwable root = getRootCause(throwable);
        String rootMessage = root.getMessage();

        if (throwable instanceof CannotGetJdbcConnectionException
                || root instanceof CannotGetJdbcConnectionException
                || contains(rootMessage, "Access denied for user")
                || contains(rootMessage, "Communications link failure")
                || contains(rootMessage, "Failed to obtain JDBC Connection")) {
            return "数据库连接失败，请检查 backend/application-local.yml 或 DB_PASSWORD 配置";
        }

        if (contains(rootMessage, "doesn't exist") || contains(rootMessage, "Unknown table")) {
            return "数据库表不存在，请先执行 docs/sql/init.sql 初始化数据库";
        }

        if (rootMessage != null && !rootMessage.isBlank()) {
            return rootMessage;
        }

        return "操作失败";
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
