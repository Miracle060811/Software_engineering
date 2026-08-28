package com.travelmate.microservices.identity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({"com.travelmate.backend.mapper", "com.travelmate.mapper"})
@SpringBootApplication(scanBasePackages = "com.travelmate")
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
