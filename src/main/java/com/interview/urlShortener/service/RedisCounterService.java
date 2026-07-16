package com.interview.urlShortener.service;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@Profile("redis")
public class RedisCounterService implements CounterService {

    private final StringRedisTemplate redisTemplate;
    private static final String COUNTER_KEY = "global_url_counter";
    private static final long RANGE_SIZE = 1000L;

    private final AtomicLong currentVal = new AtomicLong(0);
    private long maxVal = 0;

    public RedisCounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public synchronized long getNextId() {
        // If current local counter hasn't been initialized or has exhausted its range, fetch next range.
        if (currentVal.get() == 0 || currentVal.get() > maxVal) {
            allocateNextSegment();
        }
        return currentVal.getAndIncrement();
    }

    private void allocateNextSegment() {
        // Atomically increment the global counter in Redis by range size
        Long nextMax = redisTemplate.opsForValue().increment(COUNTER_KEY, RANGE_SIZE);
        if (nextMax == null) {
            throw new IllegalStateException("Failed to increment global counter in Redis.");
        }
        
        long startVal = nextMax - RANGE_SIZE + 1;
        currentVal.set(startVal);
        maxVal = nextMax;
    }
}
