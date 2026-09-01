package com.travelmate.service;

public interface HotelRoomStockService {

    StockPreDeductResult preDeductRoom(Long roomId, Integer dbAvailableRooms, Integer count);

    void rollbackPreDeduct(Long roomId, Integer count);

    void syncWithDatabase(Long roomId);
}
