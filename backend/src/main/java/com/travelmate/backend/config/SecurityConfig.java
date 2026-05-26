package com.travelmate.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
public class SecurityConfig {

        @Autowired
        private JwtFilter jwtFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http.csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/user/register", "/user/login",
                                                                "/user/reset-password",
                                                                "/favicon.ico",
                                                                "/assets/**",
                                                                "/images/**",
                                                                "/uploads/**",
                                                                "/vite.svg")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/flight/**",
                                                                "/api/train/**",
                                                                "/api/hotel/search",
                                                                "/api/attraction/search",
                                                                "/api/review/list",
                                                                "/api/coupon/list")
                                                .permitAll()
                                                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET,
                                                                "^/api/hotel/\\d+$"))
                                                .permitAll()
                                                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET,
                                                                "^/api/hotel/\\d+/rooms$"))
                                                .permitAll()
                                                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET,
                                                                "^/api/attraction/\\d+$"))
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/post/list")
                                                .permitAll()
                                                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET,
                                                                "^/api/post/\\d+$"))
                                                .permitAll()
                                                .requestMatchers("/api/admin/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/post/**")
                                                .authenticated()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }
}
