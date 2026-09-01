package com.travelmate.microservices.identity;

import com.travelmate.backend.mapper.UserMapper;
import com.travelmate.mapper.FollowMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalCommunityIdentityControllerTests {
    @Test
    void invalidInternalTokenIsForbidden() throws Exception {
        InternalCommunityIdentityController controller = new InternalCommunityIdentityController(
                mock(UserMapper.class), mock(FollowMapper.class), "shared-token");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/internal/identity/community/users/1")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
    }
}
