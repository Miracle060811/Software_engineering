package com.travelmate.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
public class SecurityConfig {

        @Autowired
        private JwtFilter jwtFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
                csrfRequestHandler.setCsrfRequestAttributeName(null);

                http.csrf(csrf -> csrf
                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .csrfTokenRequestHandler(csrfRequestHandler)
                                .ignoringRequestMatchers(
                                                "/user/register",
                                                "/user/login",
                                                "/user/admin-register",
                                                "/user/reset-password",
                                                "/internal/**"))
                                .cors(Customizer.withDefaults())
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.GET,
                                                                "/",
                                                                "/index.html",
                                                                "/login",
                                                                "/destinations",
                                                                "/destination/*",
                                                                "/about",
                                                                "/terms",
                                                                "/privacy",
                                                                "/help",
                                                                "/flight-search",
                                                                "/train-search",
                                                                "/hotel-search",
                                                                "/hotel/*",
                                                                "/attractions",
                                                                "/ai-plan",
                                                                "/community",
                                                                "/post/create",
                                                                "/post/*",
                                                                "/my-orders",
                                                                "/coupons",
                                                                "/notifications",
                                                                "/messages",
                                                                "/collections",
                                                                "/profile/*",
                                                                "/admin")
                                                .permitAll()
                                                .requestMatchers(
                                                                "/user/register", "/user/login",
                                                                "/user/admin-register",
                                                                "/user/reset-password",
                                                                "/user/refresh", "/user/logout",
                                                "/internal/**",
                                                "/actuator/health",
                                                                "/actuator/health/**",
                                                                "/favicon.ico",
                                                                "/assets/**",
                                                                "/images/**",
                                                                "/uploads/**",
                                                                "/vite.svg")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/flight/**",
                                                                "/api/train/**",
                                                                "/api/destinations",
                                                                "/api/destinations/**",
                                                                "/api/hotel/search",
                                                                "/api/attraction/search",
                                                                "/api/tour/list",
                                                                "/api/review/list",
                                                                "/api/coupon/list",
                                                                "/api/user/profile/**",
                                                                "/api/comment/list")
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
                                                .requestMatchers("/api/private-message/**")
                                                .authenticated()
                                                .requestMatchers("/api/post/**")
                                                .authenticated()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }
}
