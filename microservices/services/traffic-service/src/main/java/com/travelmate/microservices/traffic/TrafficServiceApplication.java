package com.travelmate.microservices.traffic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.travelmate.mapper")
@SpringBootApplication(scanBasePackages = "com.travelmate")
@EnableScheduling
public class TrafficServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrafficServiceApplication.class, args);
    }
}
