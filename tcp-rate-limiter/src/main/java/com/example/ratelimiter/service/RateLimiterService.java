package com.example.ratelimiter.service;

import redis.clients.jedis.Jedis;
import com.example.ratelimiter.redis.RedisClient;

public class RateLimiterService {

    private static final int LIMIT = 10;
    private static final int WINDOW_SECONDS = 60;

    public boolean allowRequest(String clientId) {

        String key = "rate_limit:" + clientId;
        Jedis jedis = RedisClient.getInstance();

        long requestCount = jedis.incr(key);

        if (requestCount == 1) {
            jedis.expire(key, WINDOW_SECONDS);
        }

        return requestCount <= LIMIT;
    }
}
