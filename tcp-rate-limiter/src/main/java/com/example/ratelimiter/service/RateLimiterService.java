package com.example.ratelimiter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 5;

    // Thread-safe pool
    private static final JedisPool jedisPool = new JedisPool("localhost", 6379);

    public boolean allowRequest(String clientIp) {
        String key = "rate:" + clientIp;

        try (Jedis jedis = jedisPool.getResource()) {
            long count = jedis.incr(key);

            if (count == 1) {
                jedis.expire(key, WINDOW_SECONDS);
            }

            boolean allowed = count <= MAX_REQUESTS;

            if (!allowed) {
                logger.warn(
                    "Request denied | clientIp={} | reason=RATE_LIMIT_EXCEEDED | count={}",
                    clientIp, count
                );
            }

            return allowed;
        }
    }
}
