package com.travelmate.microservices.ops;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpsAggregationGateway {
    private final RestClient identity;
    private final RestClient traffic;
    private final RestClient local;
    private final RestClient community;
    private final String token;

    public OpsAggregationGateway(RestClient.Builder builder,
                                 @Value("${app.services.identity-url}") String identityUrl,
                                 @Value("${app.services.traffic-url}") String trafficUrl,
                                 @Value("${app.services.local-url}") String localUrl,
                                 @Value("${app.services.community-url}") String communityUrl,
                                 @Value("${app.internal-service-token}") String token,
                                 @Value("${app.services.connect-timeout-ms:1000}") int connectTimeoutMs,
                                 @Value("${app.services.read-timeout-ms:2000}") int readTimeoutMs) {
        this.identity = buildClient(builder, identityUrl, connectTimeoutMs, readTimeoutMs);
        this.traffic = buildClient(builder, trafficUrl, connectTimeoutMs, readTimeoutMs);
        this.local = buildClient(builder, localUrl, connectTimeoutMs, readTimeoutMs);
        this.community = buildClient(builder, communityUrl, connectTimeoutMs, readTimeoutMs);
        this.token = token;
    }

    public List<Map<String, Object>> users() { return list(identity, "/internal/identity/admin/users"); }
    public List<Map<String, Object>> orders() { return list(traffic, "/internal/traffic/admin/orders"); }
    public List<Map<String, Object>> flights() { return list(traffic, "/internal/traffic/admin/flights"); }
    public List<Map<String, Object>> posts() { return list(community, "/internal/community/admin/posts"); }
    public long pendingPostCount() { return scalar(community, "/internal/community/admin/pending-post-count"); }
    public List<Map<String, Object>> reviewReports(Integer status) {
        String path = status == null ? "/internal/local/admin/review-reports"
                : "/internal/local/admin/review-reports?status=" + status;
        return list(local, path);
    }

    public Map<String, Object> approvePost(Long id) { return post(community, "/internal/community/admin/posts/" + id + "/approve", Map.of()); }
    public Map<String, Object> resolveReport(Long id, Map<String, Object> body) { return post(local, "/internal/local/admin/review-reports/" + id + "/resolve", body); }

    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", scalar(identity, "/internal/identity/admin/count"));
        result.put("totalOrders", scalar(traffic, "/internal/traffic/admin/order-count"));
        result.put("pendingPosts", scalar(community, "/internal/community/admin/pending-post-count"));
        result.put("pendingReports", scalar(local, "/internal/local/admin/pending-report-count"));
        return result;
    }

    private List<Map<String, Object>> list(RestClient client, String path) {
        try {
            List<Map<String, Object>> body = client.get().uri(path).header("X-Internal-Token", token)
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            return body == null ? List.of() : body;
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private Map<String, Object> post(RestClient client, String path, Object body) {
        try {
            Map<String, Object> result = client.post().uri(path).header("X-Internal-Token", token).body(body)
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            return result == null ? Map.of() : result;
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private long scalar(RestClient client, String path) {
        try {
            Number result = client.get().uri(path).header("X-Internal-Token", token).retrieve().body(Number.class);
            return result == null ? 0 : result.longValue();
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private ResponseStatusException unavailable(RestClientException cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "业务服务暂不可用，请稍后重试", cause);
    }

    private RestClient buildClient(RestClient.Builder builder, String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException("内部服务超时时间必须大于 0");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return builder.clone().requestFactory(requestFactory).baseUrl(baseUrl).build();
    }
}
