package com.example.ratelimiter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    private static final int MAX_REQUESTS = intConfig("RATE_LIMIT_MAX_REQUESTS", "rate.maxRequests", 10);
    private static final int WINDOW_SECONDS = intConfig("RATE_LIMIT_WINDOW_SECONDS", "rate.windowSeconds", 60);
    private static final String REDIS_HOST = stringConfig("REDIS_HOST", "redis.host", "localhost");
    private static final int REDIS_PORT = intConfig("REDIS_PORT", "redis.port", 6379);

    private final RateLimiter limiter;

    public boolean allowRequest(String clientIp) {
        String key = "rate:" + clientIp;

        boolean allowed = limiter.allow(key);

        if (!allowed) {
            logger.warn(
                "Request denied | clientIp={} | reason=RATE_LIMIT_EXCEEDED",
                clientIp
            );
        }

        return allowed;
    }

    public RateLimiterService() {
        this(new SlidingWindowRedisRateLimiter(REDIS_HOST, REDIS_PORT, MAX_REQUESTS, WINDOW_SECONDS));
    }

    RateLimiterService(RateLimiter limiter) {
        this.limiter = limiter;
    }

    public void close() {
        limiter.close();
    }

    private static int intConfig(String envKey, String propKey, int defaultValue) {
        String value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propKey);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid int config for {} / {}: {}", envKey, propKey, value);
            return defaultValue;
        }
    }

    private static String stringConfig(String envKey, String propKey, String defaultValue) {
        String value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propKey);
        }
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }
}
