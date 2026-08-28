package com.travelmate.microservices.traffic;

import com.travelmate.integration.CouponGateway;
import com.travelmate.integration.PassengerGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class TrafficIntegrationGateway implements PassengerGateway, CouponGateway {

    private final RestClient identityClient;
    private final RestClient localClient;
    private final String serviceToken;

    public TrafficIntegrationGateway(RestClient.Builder builder,
                                     @Value("${app.services.identity-url}") String identityUrl,
                                     @Value("${app.services.local-url}") String localUrl,
                                     @Value("${app.internal-service-token}") String serviceToken) {
        this.identityClient = builder.clone().baseUrl(identityUrl).build();
        this.localClient = builder.clone().baseUrl(localUrl).build();
        this.serviceToken = serviceToken;
    }

    @Override
    public PassengerSnapshot findOwnedPassenger(Long passengerId, Long userId) {
        return identityClient.get()
                .uri(uri -> uri.path("/internal/identity/passengers/{id}/ownership")
                        .queryParam("userId", userId).build(passengerId))
                .header("X-Internal-Token", serviceToken)
                .retrieve()
                .body(PassengerSnapshot.class);
    }

    @Override
    public BigDecimal redeem(Long userId, Long userCouponId, BigDecimal amount, String businessType) {
        if (userCouponId == null) {
            return amount;
        }
        BigDecimal payable = localClient.post()
                .uri("/internal/local/coupons/redeem")
                .header("X-Internal-Token", serviceToken)
                .body(new CouponRedemption(userId, userCouponId, amount, businessType))
                .retrieve()
                .body(BigDecimal.class);
        if (payable == null) {
            throw new IllegalStateException("优惠券服务未返回应付金额");
        }
        return payable;
    }

    private record CouponRedemption(Long userId, Long userCouponId, BigDecimal amount, String businessType) {
    }
}
