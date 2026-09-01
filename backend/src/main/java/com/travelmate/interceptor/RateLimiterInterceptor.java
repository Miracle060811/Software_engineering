package com.travelmate.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.annotation.RateLimiter;
import com.travelmate.common.LogSanitizer;
import com.travelmate.common.RedisKeyConstants;
import com.travelmate.common.Result;
import com.travelmate.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterInterceptor.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserContext userContext;

    @Value("${travelmate.security.rate-limit-enabled:true}")
    private boolean rateLimitEnabled = true;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        if (!rateLimitEnabled) {
            return true;
        }

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimiter annotation = handlerMethod.getMethodAnnotation(RateLimiter.class);
        if (annotation == null) {
            return true;
        }

        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        String key = buildRateLimitKey(ip, uri);

        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, annotation.timeWindowSeconds(), TimeUnit.SECONDS);
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, skipping rate limit for {}", LogSanitizer.singleLine(uri));
            return true;
        }

        if (count != null && count > annotation.maxRequests()) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);
            try {
                response.getWriter().write(new ObjectMapper().writeValueAsString(
                        Result.error("请求过于频繁，请稍后再试")));
            } catch (IOException ignored) {
            }
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildRateLimitKey(String ip, String uri) {
        Long userId = null;
        try {
            userId = userContext.getCurrentUserIdOrNull();
        } catch (RuntimeException ignored) {
        }

        if (userId != null) {
            return RedisKeyConstants.RATE_LIMIT_USER_PREFIX + userId + ":" + uri;
        }
        return RedisKeyConstants.RATE_LIMIT_IP_PREFIX + ip + ":" + uri;
    }
}
