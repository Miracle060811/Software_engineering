package com.travelmate.config;

import com.travelmate.interceptor.RateLimiterInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    @NonNull
    private RateLimiterInterceptor rateLimiterInterceptor;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rateLimiterInterceptor);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/", "file:../frontend/public/images/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .setUseLastModified(true);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        fileLocation(Path.of(uploadDir)),
                        fileLocation(Path.of("uploads")),
                        fileLocation(Path.of("backend", "uploads")),
                        fileLocation(Path.of("..", "uploads")))
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .setUseLastModified(true);
    }

    private String fileLocation(Path path) {
        return path.toAbsolutePath().normalize().toUri().toString();
    }
}
