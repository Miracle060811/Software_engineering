package com.travelmate.microservices.traffic;

import com.travelmate.integration.CouponGateway;
import com.travelmate.integration.PassengerGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Component
public class TrafficIntegrationGateway implements PassengerGateway, CouponGateway {

    private final RestClient identityClient;
    private final RestClient localClient;
    private final String serviceToken;

    @Autowired
    public TrafficIntegrationGateway(RestClient.Builder builder,
                                     @Value("${app.services.identity-url}") String identityUrl,
                                     @Value("${app.services.local-url}") String localUrl,
                                     @Value("${app.internal-service-token}") String serviceToken,
                                     @Value("${app.services.identity-connect-timeout-ms:1000}") int connectTimeoutMs,
                                     @Value("${app.services.identity-read-timeout-ms:2000}") int readTimeoutMs,
                                     @Value("${app.services.local-connect-timeout-ms:1000}") int localConnectTimeoutMs,
                                     @Value("${app.services.local-read-timeout-ms:2000}") int localReadTimeoutMs) {
        this(buildClient(builder, identityUrl, connectTimeoutMs, readTimeoutMs, "身份"),
                buildClient(builder, localUrl, localConnectTimeoutMs, localReadTimeoutMs, "本地生活"),
                serviceToken);
    }

    TrafficIntegrationGateway(RestClient identityClient, RestClient localClient, String serviceToken) {
        this.identityClient = identityClient;
        this.localClient = localClient;
        this.serviceToken = serviceToken;
    }

    @Override
    public PassengerSnapshot findOwnedPassenger(Long passengerId, Long userId) {
        try {
            return identityClient.get()
                    .uri(uri -> uri.path("/internal/identity/passengers/{id}/ownership")
                            .queryParam("userId", userId).build(passengerId))
                    .header("X-Internal-Token", serviceToken)
                    .retrieve()
                    .body(PassengerSnapshot.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "身份服务暂不可用，请稍后重试", e);
        }
    }

    @Override
    public BigDecimal redeem(Long userId, Long userCouponId, BigDecimal amount, String businessType) {
        if (userCouponId == null) {
            return amount;
        }
        try {
            BigDecimal payable = localClient.post()
                    .uri("/internal/local/coupons/redeem")
                    .header("X-Internal-Token", serviceToken)
                    .body(new CouponRedemption(userId, userCouponId, amount, businessType))
                    .retrieve()
                    .body(BigDecimal.class);
            if (payable == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "本地生活服务暂不可用，请稍后重试");
            }
            return payable;
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "本地生活服务暂不可用，请稍后重试", e);
        }
    }

    private record CouponRedemption(Long userId, Long userCouponId, BigDecimal amount, String businessType) {
    }

    private static RestClient buildClient(RestClient.Builder builder, String baseUrl,
                                          int connectTimeoutMs, int readTimeoutMs,
                                          String serviceName) {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
            throw new IllegalArgumentException(serviceName + "服务超时时间必须大于 0");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return builder.clone()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();
    }
}
