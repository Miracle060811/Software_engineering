package com.travelmate.microservices.traffic;

import com.travelmate.integration.PassengerGateway.PassengerSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.Map;

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
        gateway = new TrafficIntegrationGateway(
                builder.baseUrl("http://identity.test").build(),
                RestClient.builder().baseUrl("http://local.test").build(),
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

    @Test
    void mapsIdentityReadTimeoutToServiceUnavailable() throws Exception {
        HttpServer delayedIdentity = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        delayedIdentity.createContext("/internal/identity/passengers/9/ownership", exchange -> {
            try {
                Thread.sleep(300);
                exchange.sendResponseHeaders(200, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        delayedIdentity.start();
        try {
            TrafficIntegrationGateway timeoutGateway = new TrafficIntegrationGateway(
                    RestClient.builder(),
                    "http://127.0.0.1:" + delayedIdentity.getAddress().getPort(),
                    "http://local.test",
                    "internal-test-token",
                    100,
                    50);

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> timeoutGateway.findOwnedPassenger(9L, 7L))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> {
                        ResponseStatusException response = (ResponseStatusException) error;
                        assertThat(response.getStatusCode().value()).isEqualTo(503);
                        assertThat(response.getReason()).isEqualTo("身份服务暂不可用，请稍后重试");
                    });
        } finally {
            delayedIdentity.stop(0);
        }
    }

    @Test
    void springContextUsesTheProductionConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "app.services.identity-url", "http://identity.test",
                    "app.services.local-url", "http://local.test",
                    "app.internal-service-token", "internal-test-token",
                    "app.services.identity-connect-timeout-ms", "100",
                    "app.services.identity-read-timeout-ms", "200")));
            context.registerBean(RestClient.Builder.class, () -> RestClient.builder());
            context.register(TrafficIntegrationGateway.class);

            context.refresh();

            assertThat(context.getBean(TrafficIntegrationGateway.class)).isNotNull();
        }
    }
}
