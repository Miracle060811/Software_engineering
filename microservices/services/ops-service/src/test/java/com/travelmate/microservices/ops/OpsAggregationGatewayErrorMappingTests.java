package com.travelmate.microservices.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OpsAggregationGatewayErrorMappingTests {
    private OpsAggregationGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new OpsAggregationGateway(RestClient.builder(), new ObjectMapper(),
                "http://identity", "http://traffic", "http://local", "http://community",
                "token", 1000, 2000);
    }

    @Test
    void preservesDownstreamValidationMessageAndStatus() {
        HttpClientErrorException error = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                "{\"code\":400,\"msg\":\"有效期不能为空\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        ResponseStatusException mapped = gateway.translateFailure(error);

        assertThat(mapped.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mapped.getReason()).isEqualTo("有效期不能为空");
    }

    @Test
    void keepsBusinessConflictsDistinctFromServiceOutages() {
        HttpClientErrorException error = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY,
                "{\"code\":409,\"msg\":\"该航班已有订单，不能删除\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        ResponseStatusException mapped = gateway.translateFailure(error);

        assertThat(mapped.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mapped.getReason()).isEqualTo("该航班已有订单，不能删除");
    }

    @Test
    void mapsNetworkFailuresToServiceUnavailable() {
        ResponseStatusException mapped = gateway.translateFailure(new RestClientException("connection refused"));

        assertThat(mapped.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(mapped.getReason()).isEqualTo("业务服务暂不可用，请稍后重试");
    }
}
