package com.travelmate.microservices.identity;

import com.travelmate.backend.entity.User;
import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.common.GlobalExceptionHandler;
import com.travelmate.common.UserContext;
import com.travelmate.controller.FollowController;
import com.travelmate.controller.PassengerController;
import com.travelmate.entity.Passenger;
import com.travelmate.mapper.FollowMapper;
import com.travelmate.service.FollowService;
import com.travelmate.service.PassengerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdentityDomainApiTests {
    private PassengerService passengerService;
    private FollowService followService;
    private UserMapper userMapper;
    private FollowMapper followMapper;
    private UserContext userContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        passengerService = mock(PassengerService.class);
        followService = mock(FollowService.class);
        userMapper = mock(UserMapper.class);
        followMapper = mock(FollowMapper.class);
        userContext = mock(UserContext.class);

        PassengerController passengerController = new PassengerController();
        ReflectionTestUtils.setField(passengerController, "passengerService", passengerService);
        ReflectionTestUtils.setField(passengerController, "userContext", userContext);
        FollowController followController = new FollowController();
        ReflectionTestUtils.setField(followController, "followService", followService);
        ReflectionTestUtils.setField(followController, "userContext", userContext);

        mockMvc = MockMvcBuilders.standaloneSetup(passengerController, followController,
                        new IdentityUserProfileController(userMapper, followMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void passengerFollowAndProfileEndpointsExposeNormalContracts() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(7L);
        when(userContext.getCurrentUserId()).thenReturn(7L);
        when(passengerService.getPassengerList(7L)).thenReturn(List.of());
        when(passengerService.addPassenger(any(Passenger.class))).thenReturn(true);
        when(passengerService.deletePassenger(9L, 7L)).thenReturn(true);
        when(followService.toggleFollow(7L, 8L)).thenReturn(1);
        User user = new User();
        user.setId(8L); user.setUsername("target"); user.setNickname("目标用户");
        when(followService.fans(8L)).thenReturn(List.of(user));
        when(followService.following(8L)).thenReturn(List.of(user));
        when(followService.followStatus(7L, 8L)).thenReturn(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(followMapper.selectCount(any())).thenReturn(1L);

        mockMvc.perform(get("/api/passenger/list")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/passenger/add").contentType("application/json")
                        .content("{\"name\":\"旅客\",\"idCard\":\"110101199001011234\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/api/passenger/9")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/follow/8")).andExpect(status().isOk()).andExpect(jsonPath("$.data.followed").value(true));
        mockMvc.perform(get("/api/follow/fans/8")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].userId").value(8));
        mockMvc.perform(get("/api/follow/following/8")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/follow/status/8")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/api/user/profile/target")).andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("target"));
    }

    @Test
    void identityDomainEndpointsEnforceAuthenticationBoundaries() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(null);
        when(userContext.getCurrentUserId()).thenThrow(new RuntimeException("用户未登录或Token无效"));
        when(followService.fans(8L)).thenReturn(List.of());
        when(followService.following(8L)).thenReturn(List.of());
        User user = new User(); user.setId(8L); user.setUsername("target");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(followMapper.selectCount(any())).thenReturn(0L);

        mockMvc.perform(get("/api/passenger/list")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/passenger/add").contentType("application/json")
                        .content("{\"name\":\"旅客\",\"idCard\":\"110101199001011234\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(delete("/api/passenger/9")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(post("/api/follow/8")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/follow/status/8")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/follow/fans/8")).andExpect(status().isOk());
        mockMvc.perform(get("/api/follow/following/8")).andExpect(status().isOk());
        mockMvc.perform(get("/api/user/profile/target")).andExpect(status().isOk());
    }

    @Test
    void identityDomainEndpointsRejectMalformedInput() throws Exception {
        when(userContext.getCurrentUserIdOrNull()).thenReturn(7L);
        when(userMapper.selectOne(any())).thenReturn(null);

        mockMvc.perform(post("/api/passenger/add").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(delete("/api/passenger/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/follow/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/follow/fans/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/follow/following/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/follow/status/not-a-number")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/user/profile/missing")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500));
        mockMvc.perform(get("/api/passenger/list").param("unexpected", "ignored"))
                .andExpect(status().isOk());
    }
}
