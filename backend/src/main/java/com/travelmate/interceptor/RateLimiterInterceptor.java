package com.travelmate.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.annotation.RateLimiter;
import com.travelmate.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimiter annotation = handlerMethod.getMethodAnnotation(RateLimiter.class);
        if (annotation == null) {
            return true;
        }

        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        String key = "rate_limit:" + ip + ":" + uri;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, annotation.timeWindowSeconds(), TimeUnit.SECONDS);
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
}
