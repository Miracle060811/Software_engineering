package com.travelmate.common;

public final class RedisKeyConstants {

    public static final String RATE_LIMIT_USER_PREFIX = "rate_limit:user:";
    public static final String RATE_LIMIT_IP_PREFIX = "rate_limit:ip:";
    public static final String HOTEL_ROOM_STOCK_PREFIX = "hotel:room:stock:";

    private RedisKeyConstants() {
    }
}
