package com.travelmate.microservices.identity;

import com.travelmate.backend.config.JwtUtil;
import com.travelmate.backend.controller.UserController;
import com.travelmate.backend.entity.User;
import com.travelmate.backend.service.RefreshTokenService;
import com.travelmate.backend.service.UserService;
import com.travelmate.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdentityPublicApiTests {
    private UserService userService;
    private JwtUtil jwtUtil;
    private RefreshTokenService refreshTokenService;
    private UserController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtUtil = mock(JwtUtil.class);
        refreshTokenService = mock(RefreshTokenService.class);
        controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(controller, "refreshTokenService", refreshTokenService);
        ReflectionTestUtils.setField(controller, "adminRegisterSecret", "");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerAndLoginExposeStableResultContract() throws Exception {
        when(userService.register("api-user", "Password123!", 0)).thenReturn(true);
        when(userService.login("api-user", "Password123!")).thenReturn("jwt-token");
        User user = new User();
        user.setId(7L);
        user.setUsername("api-user");
        when(userService.getUserByUsername("api-user")).thenReturn(user);
        when(refreshTokenService.create(any(User.class), anyString(), nullable(String.class)))
                .thenReturn(new RefreshTokenService.RefreshGrant("jwt-token", "refresh-token"));
        when(refreshTokenService.getRefreshDays()).thenReturn(14);

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
                .andExpect(jsonPath("$.data").value("jwt-token"))
                .andExpect(cookie().httpOnly("TRAVELMATE_REFRESH", true))
                .andExpect(cookie().path("TRAVELMATE_REFRESH", "/user"));
    }

    @Test
    void refreshRotatesSessionAndLogoutRevokesIt() throws Exception {
        when(refreshTokenService.rotate(anyString(), anyString(), nullable(String.class)))
                .thenReturn(new RefreshTokenService.RefreshGrant("new-access-token", "new-refresh-token"));
        when(refreshTokenService.getRefreshDays()).thenReturn(14);

        mockMvc.perform(post("/user/refresh").cookie(new Cookie("TRAVELMATE_REFRESH", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("new-access-token"))
                .andExpect(cookie().value("TRAVELMATE_REFRESH", "new-refresh-token"));

        mockMvc.perform(post("/user/logout").cookie(new Cookie("TRAVELMATE_REFRESH", "new-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        verify(refreshTokenService).revoke("new-refresh-token");
    }

    @Test
    void refreshWithoutCookieReturnsExpiredSessionContract() throws Exception {
        mockMvc.perform(post("/user/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
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

    @Test
    void adminRegisterAcceptsJsonAndChecksConfiguredSecret() throws Exception {
        ReflectionTestUtils.setField(controller, "adminRegisterSecret", "test-admin-secret");
        when(userService.register("admin-user", "Password123!", 1)).thenReturn(true);

        mockMvc.perform(post("/user/admin-register")
                        .contentType("application/json")
                        .content("{\"username\":\"admin-user\",\"password\":\"Password123!\",\"secret\":\"test-admin-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void accountLifecycleEndpointsExposeNormalContracts() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUsername("api-user");
        when(userService.resetPassword("api-user", "NewPassword123!")).thenReturn(true);
        when(jwtUtil.extractUsername("jwt-token")).thenReturn("api-user");
        when(userService.getUserByUsername("api-user")).thenReturn(user);
        when(userService.changePassword(7L, "Password123!", "NewPassword123!")).thenReturn(true);
        when(userService.deleteAccount(7L, "NewPassword123!")).thenReturn(true);

        mockMvc.perform(post("/user/reset-password")
                        .param("username", "api-user")
                        .param("newPassword", "NewPassword123!"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/user/password")
                        .header("Authorization", "Bearer jwt-token")
                        .param("oldPassword", "Password123!")
                        .param("newPassword", "NewPassword123!"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/user/account")
                        .header("Authorization", "Bearer jwt-token")
                        .param("password", "NewPassword123!"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void protectedAccountEndpointsRejectMissingAuthorization() throws Exception {
        mockMvc.perform(post("/user/password")
                        .param("oldPassword", "Password123!")
                        .param("newPassword", "NewPassword123!"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/user/account").param("password", "Password123!"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/user/me"))
                .andExpect(status().isBadRequest());

        ReflectionTestUtils.setField(controller, "adminRegisterSecret", "test-admin-secret");
        mockMvc.perform(post("/user/admin-register")
                        .contentType("application/json")
                        .content("{\"username\":\"admin-user\",\"password\":\"Password123!\",\"secret\":\"wrong\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void accountEndpointsRejectMissingOrMalformedParameters() throws Exception {
        mockMvc.perform(post("/user/register").param("username", "api-user"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/user/login").param("password", "Password123!"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/user/reset-password").param("username", "api-user"))
                .andExpect(status().isBadRequest());
        ReflectionTestUtils.setField(controller, "adminRegisterSecret", "test-admin-secret");
        mockMvc.perform(post("/user/admin-register").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/user/password")
                        .header("Authorization", "Bearer jwt-token")
                        .param("newPassword", "NewPassword123!"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/user/account").header("Authorization", "Bearer jwt-token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/user/me").header("Authorization", "bad"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
    }
}
