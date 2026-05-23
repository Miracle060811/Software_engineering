package com.travelmate.service.impl;

import com.travelmate.entity.HotelRoom;
import com.travelmate.mapper.HotelRoomMapper;
import com.travelmate.common.RedisKeyConstants;
import com.travelmate.service.HotelRoomStockService;
import com.travelmate.service.StockPreDeductResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class HotelRoomStockServiceImpl implements HotelRoomStockService {

    private static final Logger log = LoggerFactory.getLogger(HotelRoomStockServiceImpl.class);

    private static final long STOCK_NOT_INITIALIZED = -2L;

    private static final @NonNull DefaultRedisScript<Long> PRE_DEDUCT_SCRIPT = new DefaultRedisScript<>(
            """
                    local current = redis.call('GET', KEYS[1])
                    if not current then
                        return -2
                    end
                    current = tonumber(current)
                    local count = tonumber(ARGV[1])
                    if current < count then
                        return -1
                    end
                    redis.call('DECRBY', KEYS[1], count)
                    return current - count
                    """,
            Long.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private HotelRoomMapper hotelRoomMapper;

    @Override
    public StockPreDeductResult preDeductRoom(Long roomId, Integer dbAvailableRooms, Integer count) {
        int deductCount = count == null ? 1 : count;
        if (dbAvailableRooms == null || dbAvailableRooms < deductCount) {
            return StockPreDeductResult.NO_STOCK;
        }

        String stockKey = Objects.requireNonNull(buildStockKey(roomId));
        try {
            initializeStockIfAbsent(stockKey, dbAvailableRooms);

            Long result = executePreDeduct(stockKey, deductCount);
            if (result != null && result == STOCK_NOT_INITIALIZED) {
                initializeStockIfAbsent(stockKey, dbAvailableRooms);
                result = executePreDeduct(stockKey, deductCount);
            }

            if (result == null) {
                throw new RuntimeException("Redis库存服务异常");
            }

            return result >= 0 ? StockPreDeductResult.DEDUCTED_IN_REDIS : StockPreDeductResult.NO_STOCK;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, falling back to database stock deduction for room {}", roomId);
            return StockPreDeductResult.FALLBACK_TO_DB;
        }
    }

    @Override
    public void rollbackPreDeduct(Long roomId, Integer count) {
        String stockKey = Objects.requireNonNull(buildStockKey(roomId));
        try {
            redisTemplate.opsForValue().increment(stockKey, count == null ? 1 : count);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, skip rollback stock cache for room {}", roomId);
        }
    }

    @Override
    public void syncWithDatabase(Long roomId) {
        HotelRoom room = hotelRoomMapper.selectById(roomId);
        String stockKey = Objects.requireNonNull(buildStockKey(roomId));
        try {
            if (room == null) {
                redisTemplate.delete(stockKey);
                return;
            }

            int availableRooms = room.getAvailableRooms() == null ? 0 : Math.max(room.getAvailableRooms(), 0);
            String stockValue = Objects.requireNonNull(String.valueOf(availableRooms));
            redisTemplate.opsForValue().set(stockKey, stockValue);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, skip syncing stock cache for room {}", roomId);
        }
    }

    @SuppressWarnings("null")
    private void initializeStockIfAbsent(String stockKey, Integer dbAvailableRooms) {
        int initialStock = Math.max(dbAvailableRooms, 0);
        String stockValue = Objects.requireNonNull(String.valueOf(initialStock));
        redisTemplate.opsForValue().setIfAbsent(stockKey, stockValue);
    }

    @SuppressWarnings("null")
    private Long executePreDeduct(@NonNull String stockKey, Integer count) {
        List<String> keys = Collections.singletonList(Objects.requireNonNull(stockKey));
        return redisTemplate.execute(PRE_DEDUCT_SCRIPT, keys, String.valueOf(count));
    }

    private @NonNull String buildStockKey(Long roomId) {
        return RedisKeyConstants.HOTEL_ROOM_STOCK_PREFIX + roomId;
    }
}
