package com.checkbook.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiting service to prevent abuse of authentication endpoints
 * Uses sliding window algorithm with in-memory storage
 */
@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    @Autowired
    private ConcurrentMap<String, Object> rateLimitStorage;

    /**
     * Rate limit configuration for different endpoints
     */
    public static class RateLimit {
        public final int maxRequests;
        public final long windowMinutes;
        public final String description;

        public RateLimit(int maxRequests, long windowMinutes, String description) {
            this.maxRequests = maxRequests;
            this.windowMinutes = windowMinutes;
            this.description = description;
        }
    }

    // Rate limiting rules
    public static final RateLimit LOGIN_RATE_LIMIT = new RateLimit(5, 15, "Login attempts");
    public static final RateLimit REGISTER_RATE_LIMIT = new RateLimit(3, 60, "Registration attempts");
    public static final RateLimit PASSWORD_RESET_RATE_LIMIT = new RateLimit(3, 60, "Password reset requests");
    public static final RateLimit PASSWORD_RESET_IP_LIMIT = new RateLimit(10, 60, "Password reset requests per IP");

    /**
     * Check if request is allowed under rate limit
     */
    public boolean isAllowed(String identifier, RateLimit rateLimit) {
        String key = generateKey(identifier, rateLimit);

        try {
            RequestWindow window = (RequestWindow) rateLimitStorage.get(key);
            LocalDateTime now = LocalDateTime.now();

            if (window == null) {
                // First request
                window = new RequestWindow(now, 1);
                rateLimitStorage.put(key, window);
                logger.debug("First request for {}: {}", rateLimit.description, identifier);
                return true;
            }

            // Clean old requests outside the window
            LocalDateTime windowStart = now.minus(rateLimit.windowMinutes, ChronoUnit.MINUTES);

            if (window.startTime.isBefore(windowStart)) {
                // Window has expired, reset
                window = new RequestWindow(now, 1);
                rateLimitStorage.put(key, window);
                logger.debug("Rate limit window reset for {}: {}", rateLimit.description, identifier);
                return true;
            }

            // Check if within rate limit
            if (window.requestCount >= rateLimit.maxRequests) {
                logger.warn("Rate limit exceeded for {}: {} ({})", rateLimit.description, identifier, window.requestCount);
                return false;
            }

            // Increment counter
            window.requestCount++;
            rateLimitStorage.put(key, window);
            logger.debug("Request allowed for {}: {} ({}/{})",
                rateLimit.description, identifier, window.requestCount, rateLimit.maxRequests);

            return true;

        } catch (Exception e) {
            logger.error("Error checking rate limit for {}: {}", identifier, e.getMessage());
            // Fail open - allow request if rate limiting service fails
            return true;
        }
    }

    /**
     * Get remaining requests for identifier
     */
    public int getRemainingRequests(String identifier, RateLimit rateLimit) {
        String key = generateKey(identifier, rateLimit);

        try {
            RequestWindow window = (RequestWindow) rateLimitStorage.get(key);
            if (window == null) {
                return rateLimit.maxRequests;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime windowStart = now.minus(rateLimit.windowMinutes, ChronoUnit.MINUTES);

            if (window.startTime.isBefore(windowStart)) {
                return rateLimit.maxRequests;
            }

            return Math.max(0, rateLimit.maxRequests - window.requestCount);
        } catch (Exception e) {
            logger.error("Error getting remaining requests for {}: {}", identifier, e.getMessage());
            return rateLimit.maxRequests;
        }
    }

    /**
     * Get time until rate limit resets (in minutes)
     */
    public long getResetTimeMinutes(String identifier, RateLimit rateLimit) {
        String key = generateKey(identifier, rateLimit);

        try {
            RequestWindow window = (RequestWindow) rateLimitStorage.get(key);
            if (window == null) {
                return 0;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime resetTime = window.startTime.plus(rateLimit.windowMinutes, ChronoUnit.MINUTES);

            if (resetTime.isAfter(now)) {
                return ChronoUnit.MINUTES.between(now, resetTime);
            }

            return 0;
        } catch (Exception e) {
            logger.error("Error getting reset time for {}: {}", identifier, e.getMessage());
            return 0;
        }
    }

    /**
     * Clear rate limit for identifier (for testing or admin override)
     */
    public void clearRateLimit(String identifier, RateLimit rateLimit) {
        String key = generateKey(identifier, rateLimit);
        rateLimitStorage.remove(key);
        logger.info("Rate limit cleared for {}: {}", rateLimit.description, identifier);
    }

    /**
     * Clean up expired entries (should be called periodically)
     */
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minus(24, ChronoUnit.HOURS);

        rateLimitStorage.entrySet().removeIf(entry -> {
            try {
                RequestWindow window = (RequestWindow) entry.getValue();
                return window.startTime.isBefore(cutoff);
            } catch (Exception e) {
                // Remove invalid entries
                return true;
            }
        });

        logger.debug("Rate limit cleanup completed. Remaining entries: {}", rateLimitStorage.size());
    }

    private String generateKey(String identifier, RateLimit rateLimit) {
        return String.format("rate_limit:%s:%s", rateLimit.description.replaceAll(" ", "_"), identifier);
    }

    /**
     * Internal class to track request windows
     */
    private static class RequestWindow {
        public LocalDateTime startTime;
        public int requestCount;

        public RequestWindow(LocalDateTime startTime, int requestCount) {
            this.startTime = startTime;
            this.requestCount = requestCount;
        }
    }
}