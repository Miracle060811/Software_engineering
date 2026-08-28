package com.travelmate.microservices.identity;

import com.travelmate.entity.Passenger;
import com.travelmate.integration.PassengerGateway.PassengerSnapshot;
import com.travelmate.mapper.PassengerMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/identity")
public class InternalIdentityController {
    private final PassengerMapper passengerMapper;
    private final String serviceToken;

    public InternalIdentityController(PassengerMapper passengerMapper,
                                      @Value("${app.internal-service-token}") String serviceToken) {
        this.passengerMapper = passengerMapper;
        this.serviceToken = serviceToken;
    }

    @GetMapping("/passengers/{passengerId}/ownership")
    public PassengerSnapshot ownsPassenger(@PathVariable Long passengerId,
                                           @RequestParam Long userId,
                                           @RequestHeader("X-Internal-Token") String token) {
        verify(token);
        Passenger passenger = passengerMapper.selectById(passengerId);
        if (passenger == null || !userId.equals(passenger.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "乘车人不存在或不属于当前用户");
        }
        return new PassengerSnapshot(passenger.getId(), passenger.getName(), passenger.getIdCard());
    }

    private void verify(String token) {
        if (!serviceToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "内部服务凭证无效");
        }
    }
}
