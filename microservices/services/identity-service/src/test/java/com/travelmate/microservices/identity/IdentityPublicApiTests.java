package com.travelmate.microservices.identity;

import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.controller.UserController;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdentityPublicApiTests {
    private UserService userService;
    private JwtUtil jwtUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtUtil = mock(JwtUtil.class);
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(controller, "adminRegisterSecret", "");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void registerAndLoginExposeStableResultContract() throws Exception {
        when(userService.register("api-user", "Password123!", 0)).thenReturn(true);
        when(userService.login("api-user", "Password123!")).thenReturn("jwt-token");

        mockMvc.perform(post("/user/register")
                        .param("username", "api-user")
                        .param("password", "Password123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/user/login")
                        .param("username", "api-user")
                        .param("password", "Password123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("jwt-token"));
    }

    @Test
    void meNeverReturnsPassword() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUsername("api-user");
        user.setPassword("encoded-secret");
        when(jwtUtil.extractUsername("jwt-token")).thenReturn("api-user");
        when(userService.getUserByUsername("api-user")).thenReturn(user);

        mockMvc.perform(get("/user/me").header("Authorization", "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("api-user"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }
}
