package com.interview.urlShortener.config;

import com.interview.urlShortener.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final Map<String, TokenBucket> limiters = new ConcurrentHashMap<>();

    // Rate Limit: 10 requests per minute per IP address
    // Refill Rate: 10 tokens / 60 seconds = 0.166 tokens per second
    private static final long BUCKET_CAPACITY = 10;
    private static final double REFILL_RATE = 10.0 / 60.0;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only rate limit URL shorten (write) requests
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            return true;
        }

        String ip = getClientIP(request);
        TokenBucket bucket = limiters.computeIfAbsent(ip, k -> new TokenBucket(BUCKET_CAPACITY, REFILL_RATE));

        if (!bucket.tryConsume()) {
            response.setStatus(429); // Too Many Requests
            response.setHeader("Retry-After", "60"); // Advise to wait 60 seconds
            throw new RateLimitExceededException("Rate limit exceeded. You are allowed a maximum of " + BUCKET_CAPACITY + " requests per minute.");
        }

        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
