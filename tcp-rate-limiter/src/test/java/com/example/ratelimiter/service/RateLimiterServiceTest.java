package com.example.ratelimiter.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Jedis;

class RateLimiterServiceTest {

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final int MAX_REQUESTS = 3;
    private static final int WINDOW_SECONDS = 2;

    private String lastClientId;

    @BeforeAll
    static void setupConfigAndCheckRedis() {
        System.setProperty("redis.host", REDIS_HOST);
        System.setProperty("redis.port", String.valueOf(REDIS_PORT));
        System.setProperty("rate.maxRequests", String.valueOf(MAX_REQUESTS));
        System.setProperty("rate.windowSeconds", String.valueOf(WINDOW_SECONDS));

        assumeTrue(isRedisAvailable(REDIS_HOST, REDIS_PORT),
            "Redis is not available on localhost:6379");
    }

    @AfterEach
    void cleanupKey() {
        if (lastClientId == null) {
            return;
        }
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            jedis.del("rate:" + lastClientId);
        }
    }

    @Test
    void allowsUpToLimitAndDeniesAfter() {
        RateLimiterService service = new RateLimiterService();
        String clientId = uniqueClientId();

        assertTrue(service.allowRequest(clientId));
        assertTrue(service.allowRequest(clientId));
        assertTrue(service.allowRequest(clientId));
        assertFalse(service.allowRequest(clientId));
    }

    @Test
    void resetsAfterWindowExpires() throws InterruptedException {
        RateLimiterService service = new RateLimiterService();
        String clientId = uniqueClientId();

        assertTrue(service.allowRequest(clientId));
        assertTrue(service.allowRequest(clientId));
        assertTrue(service.allowRequest(clientId));
        assertFalse(service.allowRequest(clientId));

        Thread.sleep(Duration.ofSeconds(WINDOW_SECONDS).toMillis() + 200);

        assertTrue(service.allowRequest(clientId));
    }

    private String uniqueClientId() {
        lastClientId = "test-" + UUID.randomUUID();
        return lastClientId;
    }

    private static boolean isRedisAvailable(String host, int port) {
        try (Jedis jedis = new Jedis(host, port)) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }
}
