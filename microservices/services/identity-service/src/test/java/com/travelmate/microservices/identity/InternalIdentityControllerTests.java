package com.travelmate.microservices.identity;

import com.travelmate.entity.Passenger;
import com.travelmate.mapper.PassengerMapper;
import com.travelmate.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalIdentityControllerTests {
    private PassengerMapper passengerMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        passengerMapper = mock(PassengerMapper.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new InternalIdentityController(passengerMapper, "internal-test-token"))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsOwnedPassengerSnapshotForTrafficService() throws Exception {
        Passenger passenger = new Passenger();
        passenger.setId(9L);
        passenger.setUserId(7L);
        passenger.setName("测试旅客");
        passenger.setIdCard("110101199001011234");
        when(passengerMapper.selectById(9L)).thenReturn(passenger);

        mockMvc.perform(get("/internal/identity/passengers/9/ownership")
                        .param("userId", "7")
                        .header("X-Internal-Token", "internal-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("测试旅客"))
                .andExpect(jsonPath("$.idCard").value("110101199001011234"));
    }

    @Test
    void rejectsInvalidInternalToken() throws Exception {
        mockMvc.perform(get("/internal/identity/passengers/9/ownership")
                        .param("userId", "7")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hidesPassengerOwnedByAnotherUser() throws Exception {
        Passenger passenger = new Passenger();
        passenger.setId(9L);
        passenger.setUserId(8L);
        when(passengerMapper.selectById(9L)).thenReturn(passenger);

        mockMvc.perform(get("/internal/identity/passengers/9/ownership")
                        .param("userId", "7")
                        .header("X-Internal-Token", "internal-test-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsMissingOrMalformedOwnershipParameters() throws Exception {
        mockMvc.perform(get("/internal/identity/passengers/9/ownership")
                        .header("X-Internal-Token", "internal-test-token"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/internal/identity/passengers/not-a-number/ownership")
                        .param("userId", "7")
                        .header("X-Internal-Token", "internal-test-token"))
                .andExpect(status().isBadRequest());
    }
}
