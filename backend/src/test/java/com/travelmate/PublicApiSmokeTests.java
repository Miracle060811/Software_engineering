package com.travelmate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TravelMateApplication.class,
        properties = "JWT_SECRET=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=")
@AutoConfigureMockMvc
class PublicApiSmokeTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicReadApisReturnUnifiedSuccessPayloads() throws Exception {
        String[] publicGetApis = {
                "/api/flight/search?depCity=北京&arrCity=上海",
                "/api/train/search?depStation=北京南&arrStation=上海虹桥",
                "/api/hotel/search?city=上海",
                "/api/attraction/search?city=北京",
                "/api/post/list?page=1&size=5",
                "/api/coupon/list"
        };

        for (String api : publicGetApis) {
            mockMvc.perform(get(api))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)))
                    .andExpect(jsonPath("$.msg", is("成功")))
                    .andExpect(jsonPath("$.data", notNullValue()));
        }
    }

    @Test
    void wrongLoginKeepsHttpOkButReturnsBusinessError() throws Exception {
        mockMvc.perform(post("/user/login")
                .param("username", "missing-user")
                .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(500)))
                .andExpect(jsonPath("$.msg", is("用户名或密码错误")));
    }

    @Test
    void authenticatedBusinessApisRejectAnonymousOrderAndAiRequests() throws Exception {
        mockMvc.perform(post("/api/hotel/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/attraction/1/ticket")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"adultCount\":1,\"guestName\":\"张三\",\"guestPhone\":\"13800138000\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"推荐北京三日游\"}"))
                .andExpect(status().isForbidden());
    }
}
