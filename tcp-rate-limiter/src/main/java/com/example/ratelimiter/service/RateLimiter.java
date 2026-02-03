package com.example.ratelimiter.service;

interface RateLimiter {
    boolean allow(String key);
    void close();
}
