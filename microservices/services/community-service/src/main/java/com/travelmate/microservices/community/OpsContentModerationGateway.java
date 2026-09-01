package com.travelmate.microservices.community;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OpsContentModerationGateway {
    private final RestClient client;
    private final String serviceToken;

    public OpsContentModerationGateway(RestClient.Builder builder,
                                       @Value("${app.services.ops-url}") String opsUrl,
                                       @Value("${app.internal-service-token}") String serviceToken) {
        this.client = builder.clone().baseUrl(opsUrl).build();
        this.serviceToken = serviceToken;
    }

    public boolean containsSensitiveWord(String content) {
        try {
            ContentCheck result = client.post().uri("/internal/ops/content/check")
                    .header("X-Internal-Token", serviceToken)
                    .body(new ContentRequest(content)).retrieve().body(ContentCheck.class);
            return result != null && result.sensitive();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "内容安全服务暂不可用，请稍后重试", e);
        }
    }

    private record ContentRequest(String content) {}
    private record ContentCheck(boolean sensitive) {}
}
