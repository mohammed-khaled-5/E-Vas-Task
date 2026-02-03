package com.example.ratelimiter.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SlidingWindowUnitTest {

    @Test
    void allowsUpToLimitWithinWindow() {
        FakeClock clock = new FakeClock();
        RateLimiter limiter = new InMemorySlidingWindowRateLimiter(clock, 3, 2000);

        assertTrue(limiter.allow("client-1"));
        assertTrue(limiter.allow("client-1"));
        assertTrue(limiter.allow("client-1"));
        assertFalse(limiter.allow("client-1"));
    }

    @Test
    void allowsAgainAfterWindowExpires() {
        FakeClock clock = new FakeClock();
        RateLimiter limiter = new InMemorySlidingWindowRateLimiter(clock, 2, 2000);

        assertTrue(limiter.allow("client-2"));
        assertTrue(limiter.allow("client-2"));
        assertFalse(limiter.allow("client-2"));

        clock.advanceMillis(2100);

        assertTrue(limiter.allow("client-2"));
    }

    private static final class FakeClock {
        private long nowMillis;

        long nowMillis() {
            return nowMillis;
        }

        void advanceMillis(long millis) {
            nowMillis += millis;
        }
    }

    private static final class InMemorySlidingWindowRateLimiter implements RateLimiter {
        private final FakeClock clock;
        private final int maxRequests;
        private final long windowMillis;
        private final Map<String, Deque<Long>> windows = new HashMap<>();

        InMemorySlidingWindowRateLimiter(FakeClock clock, int maxRequests, long windowMillis) {
            this.clock = clock;
            this.maxRequests = maxRequests;
            this.windowMillis = windowMillis;
        }

        @Override
        public boolean allow(String key) {
            long now = clock.nowMillis();
            Deque<Long> timestamps = windows.computeIfAbsent(key, k -> new ArrayDeque<>());

            while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowMillis) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }

        @Override
        public void close() {
        }
    }
}
