package com.travelmate.microservices.traffic;

import com.travelmate.integration.PassengerGateway.PassengerSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TrafficIdentityIntegrationTests {
    private MockRestServiceServer server;
    private TrafficIntegrationGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new TrafficIntegrationGateway(builder, "http://identity.test", "http://local.test",
                "internal-test-token");
    }

    @Test
    void requestsOwnedPassengerFromIdentityWithInternalCredential() {
        server.expect(requestTo("http://identity.test/internal/identity/passengers/9/ownership?userId=7"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("X-Internal-Token", "internal-test-token"))
                .andRespond(withSuccess("{\"id\":9,\"name\":\"测试旅客\",\"idCard\":\"110101199001011234\"}",
                        MediaType.APPLICATION_JSON));

        PassengerSnapshot passenger = gateway.findOwnedPassenger(9L, 7L);

        assertThat(passenger).isEqualTo(new PassengerSnapshot(9L, "测试旅客", "110101199001011234"));
        server.verify();
    }
}
