package com.travelmate.service;

public interface HotelRoomStockService {

    StockPreDeductResult preDeductRoom(Long roomId, Integer dbAvailableRooms);

    void rollbackPreDeduct(Long roomId);

    void syncWithDatabase(Long roomId);
}
