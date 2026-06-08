package com.travelmate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI travelMateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("伴游 TravelMate API")
                        .description("TravelMate 后端接口文档")
                        .version("1.0.0"));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")
                .pathsToMatch("/**")
                .build();
    }
}
