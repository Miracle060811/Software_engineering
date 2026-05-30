package com.travelmate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TravelMateApplication.class)
@AutoConfigureMockMvc
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
    void deployedFrontendOriginIsAllowedForUserApi() throws Exception {
        mockMvc.perform(options("/user/register")
                .header("Origin", "http://82.156.91.79:42356")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://82.156.91.79:42356"));
    }
}
