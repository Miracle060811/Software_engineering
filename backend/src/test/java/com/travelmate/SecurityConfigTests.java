package com.travelmate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TravelMateApplication.class,
        properties = "JWT_SECRET=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedPostCreationIsRejectedBySecurityLayer() throws Exception {
        mockMvc.perform(post("/api/post/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedFileUploadIsRejectedBySecurityLayer() throws Exception {
        mockMvc.perform(multipart("/api/file/upload")
                .file("file", "image".getBytes())
                .param("name", "photo.jpg"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedMyPostsRequestIsRejectedBySecurityLayer() throws Exception {
        mockMvc.perform(get("/api/post/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedFollowingPostsRequestIsRejectedBySecurityLayer() throws Exception {
        mockMvc.perform(get("/api/post/following"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedPostDeletionIsRejectedBySecurityLayer() throws Exception {
        mockMvc.perform(delete("/api/post/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedHotelOrdersRequestIsRejectedBySecurityLayer() throws Exception {
        mockMvc.perform(get("/api/hotel/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tourProductsCanBeBrowsedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/tour/list").param("type", "0"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedAdminRequestIsRejectedBySecurityLayer() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/data"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedStaticImageRequestIsServed() throws Exception {
        mockMvc.perform(get("/images/seed/beijing.svg"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointIsPublicAndReportsServiceStatus() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"));

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    void frontendHistoryRoutesAreForwardedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/ai-plan"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/coupons"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void deployedFrontendOriginIsAllowedForUserApi() throws Exception {
        mockMvc.perform(options("/user/register")
                .header("Origin", "http://82.156.91.79:42356")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://82.156.91.79:42356"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
