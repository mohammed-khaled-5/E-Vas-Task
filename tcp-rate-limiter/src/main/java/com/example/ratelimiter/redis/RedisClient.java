package com.example.ratelimiter.redis;

import redis.clients.jedis.Jedis;

public class RedisClient {

    private static final Jedis jedis = new Jedis("localhost", 6379);

    private RedisClient() {
    }

    public static Jedis getInstance() {
        return jedis;
    }
}
