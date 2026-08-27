package com.travelmate.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.TravelMateApplication;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Key;
import java.util.Date;

@SpringBootTest(classes = TravelMateApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("mysql-test")
@Tag("mysql-integration")
public abstract class AbstractMySQLIntegrationTest {

    private static final String JWT_SECRET = "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=";
    private static final Key JWT_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE tm_traffic_order");
        jdbcTemplate.execute("TRUNCATE TABLE tm_hotel_order");
        jdbcTemplate.execute("TRUNCATE TABLE tm_attraction_order");
        jdbcTemplate.execute("TRUNCATE TABLE tm_user_coupon");
        jdbcTemplate.execute("TRUNCATE TABLE tm_coupon");
        jdbcTemplate.execute("TRUNCATE TABLE tm_passenger");
        jdbcTemplate.execute("TRUNCATE TABLE tm_train_waitlist");
        jdbcTemplate.execute("TRUNCATE TABLE tm_notification");
        jdbcTemplate.execute("TRUNCATE TABLE tm_private_message");
        jdbcTemplate.execute("TRUNCATE TABLE tm_private_contact");
        jdbcTemplate.execute("TRUNCATE TABLE tm_comment");
        jdbcTemplate.execute("TRUNCATE TABLE tm_reply");
        jdbcTemplate.execute("TRUNCATE TABLE tm_like");
        jdbcTemplate.execute("TRUNCATE TABLE tm_follow");
        jdbcTemplate.execute("TRUNCATE TABLE tm_review");
        jdbcTemplate.execute("TRUNCATE TABLE tm_review_report");
        jdbcTemplate.execute("TRUNCATE TABLE tm_post");
        jdbcTemplate.execute("TRUNCATE TABLE tm_ai_chat");
        jdbcTemplate.execute("TRUNCATE TABLE tm_ai_plan");
        jdbcTemplate.execute("TRUNCATE TABLE tm_price_history");
        jdbcTemplate.execute("TRUNCATE TABLE tm_media_asset");
        jdbcTemplate.execute("TRUNCATE TABLE tm_tour_product_step");
        jdbcTemplate.execute("TRUNCATE TABLE tm_tour_product");
        jdbcTemplate.execute("TRUNCATE TABLE sys_log");
        jdbcTemplate.execute("TRUNCATE TABLE sys_sensitive_word");
        jdbcTemplate.execute("TRUNCATE TABLE tm_user");
        jdbcTemplate.execute("TRUNCATE TABLE tm_flight");
        jdbcTemplate.execute("TRUNCATE TABLE tm_train");
        jdbcTemplate.execute("TRUNCATE TABLE tm_hotel_room");
        jdbcTemplate.execute("TRUNCATE TABLE tm_hotel");
        jdbcTemplate.execute("TRUNCATE TABLE tm_attraction");
        jdbcTemplate.execute("TRUNCATE TABLE tm_destination");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    protected String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(JWT_KEY)
                .compact();
    }

    protected String registerAndGetToken(String username, String password) throws Exception {
        String registerBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"nickname\":\"%s\"}",
                username, password, username);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/user/register")
                .contentType("application/json")
                .content(registerBody));
        return generateToken(username);
    }
}