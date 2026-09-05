package com.travelmate.microservices.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiIdentityGateway {
    private final RestClient client;
    private final String token;

    public AiIdentityGateway(RestClient.Builder builder, @Value("${app.services.identity-url}") String identityUrl,
                             @Value("${app.internal-service-token}") String token) {
        this.client = builder.clone().baseUrl(identityUrl).build();
        this.token = token;
    }

    public boolean isAvailable(Long userId) {
        try {
            client.get().uri("/internal/identity/community/users/{id}", userId)
                    .header("X-Internal-Token", token).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用，请稍后重试", e);
        }
    }

    public List<UserSummary> searchUsers(Long currentUserId, String keyword) {
        try {
            UserSummary[] users = client.get().uri(builder -> builder
                            .path("/internal/identity/community/search")
                            .queryParam("keyword", keyword)
                            .queryParam("excludeUserId", currentUserId)
                            .build())
                    .header("X-Internal-Token", token).retrieve().body(UserSummary[].class);
            return users == null ? List.of() : List.of(users);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用，请稍后重试", e);
        }
    }

    public Map<Long, UserSummary> findUsers(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        try {
            UserSummary[] users = client.get().uri(builder -> builder
                            .path("/internal/identity/community/users")
                            .queryParam("ids", ids)
                            .build())
                    .header("X-Internal-Token", token).retrieve().body(UserSummary[].class);
            if (users == null) return Map.of();
            return List.of(users).stream().collect(Collectors.toMap(UserSummary::id, Function.identity()));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用，请稍后重试", e);
        }
    }

    public record UserSummary(Long id, String username, String nickname, String avatar, String bio) {}
}
