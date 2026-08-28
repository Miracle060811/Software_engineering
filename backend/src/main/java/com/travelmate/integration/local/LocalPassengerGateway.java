package com.travelmate.integration.local;

import com.travelmate.entity.Passenger;
import com.travelmate.integration.PassengerGateway;
import com.travelmate.mapper.PassengerMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.integration.mode", havingValue = "local", matchIfMissing = true)
public class LocalPassengerGateway implements PassengerGateway {
    private final PassengerMapper passengerMapper;

    public LocalPassengerGateway(PassengerMapper passengerMapper) {
        this.passengerMapper = passengerMapper;
    }

    @Override
    public PassengerSnapshot findOwnedPassenger(Long passengerId, Long userId) {
        Passenger passenger = passengerMapper.selectById(passengerId);
        if (passenger == null || !userId.equals(passenger.getUserId())) {
            return null;
        }
        return new PassengerSnapshot(passenger.getId(), passenger.getName(), passenger.getIdCard());
    }
}
