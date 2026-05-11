package com.travelmate.service.impl;

import com.travelmate.entity.HotelRoom;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.service.HotelRoomStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class HotelRoomStockServiceImpl implements HotelRoomStockService {

    private static final long STOCK_NOT_INITIALIZED = -2L;

    private static final @NonNull DefaultRedisScript<Long> PRE_DEDUCT_SCRIPT = new DefaultRedisScript<>(
            """
                    local current = redis.call('GET', KEYS[1])
                    if not current then
                        return -2
                    end
                    current = tonumber(current)
                    if current <= 0 then
                        return -1
                    end
                    redis.call('DECR', KEYS[1])
                    return current - 1
                    """,
            Long.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private HotelRoomMapper hotelRoomMapper;

    @Override
    public boolean preDeductRoom(Long roomId, Integer dbAvailableRooms) {
        if (dbAvailableRooms == null || dbAvailableRooms <= 0) {
            return false;
        }

        String stockKey = Objects.requireNonNull(buildStockKey(roomId));
        initializeStockIfAbsent(stockKey, dbAvailableRooms);

        Long result = executePreDeduct(stockKey);
        if (result != null && result == STOCK_NOT_INITIALIZED) {
            initializeStockIfAbsent(stockKey, dbAvailableRooms);
            result = executePreDeduct(stockKey);
        }

        if (result == null) {
            throw new RuntimeException("Redis库存服务异常");
        }

        return result >= 0;
    }

    @Override
    public void rollbackPreDeduct(Long roomId) {
        String stockKey = Objects.requireNonNull(buildStockKey(roomId));
        redisTemplate.opsForValue().increment(stockKey);
    }

    @Override
    public void syncWithDatabase(Long roomId) {
        HotelRoom room = hotelRoomMapper.selectById(roomId);
        String stockKey = Objects.requireNonNull(buildStockKey(roomId));
        if (room == null) {
            redisTemplate.delete(stockKey);
            return;
        }

        int availableRooms = room.getAvailableRooms() == null ? 0 : Math.max(room.getAvailableRooms(), 0);
        String stockValue = Objects.requireNonNull(String.valueOf(availableRooms));
        redisTemplate.opsForValue().set(stockKey, stockValue);
    }

    @SuppressWarnings("null")
    private void initializeStockIfAbsent(String stockKey, Integer dbAvailableRooms) {
        int initialStock = Math.max(dbAvailableRooms, 0);
        String stockValue = Objects.requireNonNull(String.valueOf(initialStock));
        redisTemplate.opsForValue().setIfAbsent(stockKey, stockValue);
    }

    @SuppressWarnings("null")
    private Long executePreDeduct(@NonNull String stockKey) {
        List<String> keys = Collections.singletonList(Objects.requireNonNull(stockKey));
        return redisTemplate.execute(PRE_DEDUCT_SCRIPT, keys);
    }

    private @NonNull String buildStockKey(Long roomId) {
        return "hotel:room:stock:" + roomId;
    }
}