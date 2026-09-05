package com.travelmate.microservices.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Component
public class CommunityProfileGateway {
    private final RestClient client;
    private final String serviceToken;

    public CommunityProfileGateway(RestClient.Builder builder,
                                   @Value("${app.services.community-url}") String communityUrl,
                                   @Value("${app.internal-service-token}") String serviceToken) {
        this.client = builder.clone().baseUrl(communityUrl).build();
        this.serviceToken = serviceToken;
    }

    public List<Map<String, Object>> publishedPosts(Long userId) {
        try {
            List<Map<String, Object>> posts = client.get()
                    .uri("/internal/community/users/{userId}/posts", userId)
                    .header("X-Internal-Token", serviceToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return posts == null ? List.of() : posts;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "社区服务暂不可用", exception);
        }
    }
}
