package com.travelmate.service;

public interface HotelRoomStockService {

    boolean preDeductRoom(Long roomId, Integer dbAvailableRooms);

    void rollbackPreDeduct(Long roomId);

    void syncWithDatabase(Long roomId);
}