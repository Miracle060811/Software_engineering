package com.travelmate.integration;

public interface PassengerGateway {
    PassengerSnapshot findOwnedPassenger(Long passengerId, Long userId);

    record PassengerSnapshot(Long id, String name, String idCard) {
    }
}
