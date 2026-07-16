package com.interview.urlShortener.config;

import java.time.Instant;

public class TokenBucket {
    private final long capacity;
    private final double refillRatePerSecond;
    private double tokens;
    private long lastRefillTimestamp;

    public TokenBucket(long capacity, double refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = capacity;
        this.lastRefillTimestamp = Instant.now().getEpochSecond();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = Instant.now().getEpochSecond();
        long elapsedTime = now - lastRefillTimestamp;
        if (elapsedTime > 0) {
            double refillTokens = elapsedTime * refillRatePerSecond;
            tokens = Math.min(capacity, tokens + refillTokens);
            lastRefillTimestamp = now;
        }
    }
}
