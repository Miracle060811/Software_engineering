package com.travelmate.microservices.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

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
}
