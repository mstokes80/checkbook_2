package com.checkbook.api.security;

import com.checkbook.api.service.RateLimitingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter to apply rate limiting to authentication endpoints
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // Only apply rate limiting to authentication endpoints
        if (!shouldApplyRateLimit(requestURI, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIdentifier = getClientIdentifier(request);
        String clientIP = getClientIP(request);

        RateLimitingService.RateLimit rateLimit = getRateLimitForEndpoint(requestURI);

        if (rateLimit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check rate limit for user identifier
        boolean allowed = rateLimitingService.isAllowed(clientIdentifier, rateLimit);

        // For password reset, also check IP-based rate limit
        if (allowed && requestURI.contains("/forgot-password")) {
            allowed = rateLimitingService.isAllowed(clientIP, RateLimitingService.PASSWORD_RESET_IP_LIMIT);
        }

        if (!allowed) {
            handleRateLimitExceeded(request, response, clientIdentifier, rateLimit);
            return;
        }

        // Add rate limit headers to response
        addRateLimitHeaders(response, clientIdentifier, rateLimit);

        filterChain.doFilter(request, response);
    }

    private boolean shouldApplyRateLimit(String requestURI, String method) {
        return "POST".equals(method) && (
            requestURI.endsWith("/auth/login") ||
            requestURI.endsWith("/auth/register") ||
            requestURI.endsWith("/auth/forgot-password")
        );
    }

    private RateLimitingService.RateLimit getRateLimitForEndpoint(String requestURI) {
        if (requestURI.endsWith("/auth/login")) {
            return RateLimitingService.LOGIN_RATE_LIMIT;
        } else if (requestURI.endsWith("/auth/register")) {
            return RateLimitingService.REGISTER_RATE_LIMIT;
        } else if (requestURI.endsWith("/auth/forgot-password")) {
            return RateLimitingService.PASSWORD_RESET_RATE_LIMIT;
        }
        return null;
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get username from request body for login/register
        // For simplicity, we'll use IP address as identifier
        // In production, you might want to parse the request body to get username
        return getClientIP(request);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    private void addRateLimitHeaders(HttpServletResponse response, String identifier,
                                   RateLimitingService.RateLimit rateLimit) {
        try {
            int remaining = rateLimitingService.getRemainingRequests(identifier, rateLimit);
            long resetTimeMinutes = rateLimitingService.getResetTimeMinutes(identifier, rateLimit);

            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.maxRequests));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetTimeMinutes));
            response.setHeader("X-RateLimit-Window", rateLimit.windowMinutes + " minutes");
        } catch (Exception e) {
            logger.warn("Error adding rate limit headers: {}", e.getMessage());
        }
    }

    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response,
                                       String identifier, RateLimitingService.RateLimit rateLimit)
            throws IOException {

        long resetTimeMinutes = rateLimitingService.getResetTimeMinutes(identifier, rateLimit);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("error", "Rate Limit Exceeded");
        errorResponse.put("message", String.format(
            "Too many %s. Please try again in %d minutes.",
            rateLimit.description.toLowerCase(),
            resetTimeMinutes
        ));
        errorResponse.put("path", request.getRequestURI());

        // Add rate limit info
        errorResponse.put("rateLimitInfo", Map.of(
            "limit", rateLimit.maxRequests,
            "window", rateLimit.windowMinutes + " minutes",
            "resetIn", resetTimeMinutes + " minutes"
        ));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(resetTimeMinutes * 60)); // Retry-After in seconds

        // Add rate limit headers
        addRateLimitHeaders(response, identifier, rateLimit);

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);

        logger.warn("Rate limit exceeded for {} ({}): {} - Reset in {} minutes",
            rateLimit.description, identifier, request.getRequestURI(), resetTimeMinutes);
    }
}