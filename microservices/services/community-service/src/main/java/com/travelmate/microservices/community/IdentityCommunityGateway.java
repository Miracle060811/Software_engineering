package com.travelmate.microservices.community;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IdentityCommunityGateway {
    private final RestClient client;
    private final String serviceToken;

    public IdentityCommunityGateway(RestClient.Builder builder,
                                    @Value("${app.services.identity-url}") String identityUrl,
                                    @Value("${app.internal-service-token}") String serviceToken) {
        this.client = builder.clone().baseUrl(identityUrl).build();
        this.serviceToken = serviceToken;
    }

    public UserSummary findUser(Long userId) {
        if (userId == null) return null;
        try {
            return client.get().uri("/internal/identity/community/users/{id}", userId)
                    .header("X-Internal-Token", serviceToken).retrieve().body(UserSummary.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    public Map<Long, UserSummary> findUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        try {
            List<UserSummary> users = client.get()
                    .uri(uri -> uri.path("/internal/identity/community/users")
                            .queryParam("ids", userIds).build())
                    .header("X-Internal-Token", serviceToken).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (users == null) return Collections.emptyMap();
            return users.stream().collect(Collectors.toMap(UserSummary::id, Function.identity()));
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null) return false;
        try {
            Boolean result = client.get().uri(uri -> uri.path("/internal/identity/community/follows/status")
                            .queryParam("followerId", followerId).queryParam("followeeId", followeeId).build())
                    .header("X-Internal-Token", serviceToken).retrieve().body(Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    public List<Long> followingIds(Long userId) {
        try {
            List<Long> result = client.get().uri("/internal/identity/community/following/{id}", userId)
                    .header("X-Internal-Token", serviceToken).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return result == null ? List.of() : result;
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private ResponseStatusException unavailable(RestClientException cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "身份服务暂不可用，请稍后重试", cause);
    }

    public record UserSummary(Long id, String username, String nickname, String avatar) {}
}
