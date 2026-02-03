package com.example.ratelimiter.service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

class SlidingWindowRedisRateLimiter implements RateLimiter {

    private static final String LUA_SCRIPT =
        "local key = KEYS[1]\n" +
        "local now = tonumber(ARGV[1])\n" +
        "local window = tonumber(ARGV[2])\n" +
        "local limit = tonumber(ARGV[3])\n" +
        "local member = ARGV[4]\n" +
        "redis.call('ZREMRANGEBYSCORE', key, 0, now - window)\n" +
        "local count = redis.call('ZCARD', key)\n" +
        "if count >= limit then\n" +
        "  return 0\n" +
        "end\n" +
        "redis.call('ZADD', key, now, member)\n" +
        "redis.call('EXPIRE', key, math.ceil(window / 1000))\n" +
        "return 1\n";

    private final JedisPool pool;
    private final int maxRequests;
    private final int windowSeconds;

    SlidingWindowRedisRateLimiter(String host, int port, int maxRequests, int windowSeconds) {
        this.pool = new JedisPool(host, port);
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public boolean allow(String key) {
        long nowMillis = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;
        String member = nowMillis + "-" + UUID.randomUUID();

        List<String> keys = Collections.singletonList(key);
        List<String> args = List.of(
            Long.toString(nowMillis),
            Long.toString(windowMillis),
            Integer.toString(maxRequests),
            member
        );

        try (Jedis jedis = pool.getResource()) {
            Object result = jedis.eval(LUA_SCRIPT, keys, args);
            if (result instanceof Long) {
                return (Long) result == 1L;
            }
            if (result instanceof byte[]) {
                String str = new String((byte[]) result, StandardCharsets.UTF_8);
                return "1".equals(str);
            }
            return false;
        }
    }

    @Override
    public void close() {
        pool.close();
    }
}
