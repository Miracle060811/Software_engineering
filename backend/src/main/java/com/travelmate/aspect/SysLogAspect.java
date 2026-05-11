package com.travelmate.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.entity.SysLog;
import com.travelmate.mapper.SysLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class SysLogAspect {

    @Autowired
    private SysLogMapper sysLogMapper;

    @Around("execution(* com.travelmate.controller..*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        SysLog sysLog = new SysLog();
        sysLog.setMethod(joinPoint.getSignature().toShortString());
        sysLog.setCreateTime(LocalDateTime.now());

        try {
            Object[] args = joinPoint.getArgs();
            String params = args != null && args.length > 0
                    ? new ObjectMapper().writeValueAsString(args)
                    : "";
            // 截断过长参数
            if (params.length() > 500) {
                params = params.substring(0, 500);
            }
            sysLog.setParams(params);
        } catch (Exception ignored) {
            sysLog.setParams("params serialization error");
        }

        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            sysLog.setUsername(username);
        } catch (Exception ignored) {
            sysLog.setUsername("anonymous");
        }

        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                sysLog.setIp(request.getRemoteAddr());
            }
        } catch (Exception ignored) {
        }

        Object result;
        try {
            result = joinPoint.proceed();
            sysLog.setStatus(1);
            sysLog.setTimeMs(System.currentTimeMillis() - start);
        } catch (Throwable e) {
            sysLog.setStatus(0);
            sysLog.setTimeMs(System.currentTimeMillis() - start);
            sysLog.setErrorMsg(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "unknown");
            throw e;
        } finally {
            try {
                sysLogMapper.insert(sysLog);
            } catch (Exception ignored) {
                // 日志落库失败不影响业务
            }
        }

        return result;
    }
}
