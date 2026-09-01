package com.travelmate.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.annotation.RateLimiter;
import com.travelmate.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimiterInterceptorTests {

    private RateLimiterInterceptor interceptor;
    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private UserContext userContext;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        interceptor = new RateLimiterInterceptor();
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        userContext = mock(UserContext.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ReflectionTestUtils.setField(interceptor, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(interceptor, "userContext", userContext);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/api/order/flight/create");
    }

    @Test
    void allowsRequestWhenUnderRateLimit() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        HandlerMethod handler = handlerWithRateLimiter(3, 1);
        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verify(valueOperations).increment(anyString());
    }

    @Test
    void bypassesRateLimitWhenDisabledForControlledAcceptanceEnvironment() throws Exception {
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", false);

        HandlerMethod handler = handlerWithRateLimiter(3, 1);
        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void blocksRequestWhenRateLimitExceeded() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(6L);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        HandlerMethod handler = handlerWithRateLimiter(5, 1);
        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isFalse();
        verify(response).setStatus(429);
        verify(response).setContentType("application/json;charset=UTF-8");
        assertThat(stringWriter.toString()).contains("请求过于频繁");
    }

    @Test
    void bypassesRateLimitWhenRedisUnavailable() throws Exception {
        when(valueOperations.increment(anyString())).thenThrow(new RedisConnectionFailureException("redis down"));

        HandlerMethod handler = handlerWithRateLimiter(5, 1);
        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
    }

    @Test
    void allowsRequestWhenNoRateLimiterAnnotation() throws Exception {
        HandlerMethod handler = handlerWithoutRateLimiter();
        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void bypassesRateLimitWhenDisabledByConfiguration() throws Exception {
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", false);

        HandlerMethod handler = handlerWithRateLimiter(5, 1);
        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void bindsRateLimitSwitchToApplicationSecurityConfiguration() {
        try (var context = new AnnotationConfigApplicationContext()) {
            TestPropertyValues.of("app.security.rate-limit-enabled=false").applyTo(context);
            context.registerBean("redisTemplate", RedisTemplate.class, () -> redisTemplate);
            context.registerBean(UserContext.class, () -> userContext);
            context.register(RateLimiterInterceptor.class);
            context.refresh();

            var configuredInterceptor = context.getBean(RateLimiterInterceptor.class);
            assertThat(ReflectionTestUtils.getField(configuredInterceptor, "rateLimitEnabled")).isEqualTo(false);
        }
    }

    @Test
    void allowsRequestWhenHandlerIsNotHandlerMethod() throws Exception {
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void usesClientIpFromXForwardedForHeader() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        when(valueOperations.increment(anyString())).thenReturn(1L);

        HandlerMethod handler = handlerWithRateLimiter(5, 1);
        interceptor.preHandle(request, response, handler);

        verify(valueOperations).increment(anyString());
    }

    @Test
    void usesUserIdInRateLimitKeyWhenAuthenticated() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(100L);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        HandlerMethod handler = handlerWithRateLimiter(5, 1);
        interceptor.preHandle(request, response, handler);

        verify(valueOperations).increment(anyString());
    }

    private HandlerMethod handlerWithRateLimiter(int maxRequests, int timeWindowSeconds) throws Exception {
        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("limitedMethod");
        return new HandlerMethod(controller, method);
    }

    private HandlerMethod handlerWithoutRateLimiter() throws Exception {
        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("unlimitedMethod");
        return new HandlerMethod(controller, method);
    }

    static class TestController {
        @RateLimiter(maxRequests = 5, timeWindowSeconds = 1)
        public void limitedMethod() {
        }

        public void unlimitedMethod() {
        }
    }
}
