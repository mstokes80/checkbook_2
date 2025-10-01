package com.checkbook.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Filter for logging HTTP requests and responses
 * Adds request ID for tracing and logs request details
 */
@Component
@Order(1) // Execute first
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Value("${app.logging.enabled:true}")
    private boolean loggingEnabled;

    @Value("${app.logging.log-headers:false}")
    private boolean logHeaders;

    @Value("${app.logging.log-body:false}")
    private boolean logBody;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        if (!loggingEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Generate unique request ID
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        // Add request ID to MDC for logging context
        MDC.put("requestId", requestId);

        // Add request ID header to response
        response.setHeader("X-Request-ID", requestId);

        long startTime = System.currentTimeMillis();

        try {
            logRequest(request, requestId);

            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            logResponse(request, response, requestId, duration);

            // Clean up MDC
            MDC.remove("requestId");
        }
    }

    private void logRequest(HttpServletRequest request, String requestId) {
        try {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String remoteAddr = getClientIP(request);
            String userAgent = request.getHeader("User-Agent");

            StringBuilder logMessage = new StringBuilder();
            logMessage.append("REQUEST [").append(requestId).append("] ");
            logMessage.append(method).append(" ").append(uri);

            if (queryString != null) {
                logMessage.append("?").append(queryString);
            }

            logMessage.append(" from ").append(remoteAddr);

            if (userAgent != null && !userAgent.isEmpty()) {
                logMessage.append(" (").append(userAgent).append(")");
            }

            logger.info(logMessage.toString());

            // Log headers if enabled
            if (logHeaders) {
                StringBuilder headers = new StringBuilder();
                headers.append("HEADERS [").append(requestId).append("] ");

                request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                    // Don't log sensitive headers
                    if (!isSensitiveHeader(headerName)) {
                        headers.append(headerName).append("=").append(request.getHeader(headerName)).append(", ");
                    }
                });

                if (headers.length() > 0) {
                    logger.debug(headers.toString());
                }
            }

        } catch (Exception e) {
            logger.warn("Error logging request: {}", e.getMessage());
        }
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response,
                           String requestId, long duration) {
        try {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            StringBuilder logMessage = new StringBuilder();
            logMessage.append("RESPONSE [").append(requestId).append("] ");
            logMessage.append(method).append(" ").append(uri);
            logMessage.append(" -> ").append(status);
            logMessage.append(" (").append(duration).append("ms)");

            // Log at different levels based on status code
            if (status >= 500) {
                logger.error(logMessage.toString());
            } else if (status >= 400) {
                logger.warn(logMessage.toString());
            } else {
                logger.info(logMessage.toString());
            }

            // Add response time header
            response.setHeader("X-Response-Time", duration + "ms");

        } catch (Exception e) {
            logger.warn("Error logging response: {}", e.getMessage());
        }
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

    private boolean isSensitiveHeader(String headerName) {
        String lowerCase = headerName.toLowerCase();
        return lowerCase.contains("authorization") ||
               lowerCase.contains("cookie") ||
               lowerCase.contains("x-api-key") ||
               lowerCase.contains("token");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // Don't log health check endpoints to reduce noise
        return uri.equals("/health") ||
               uri.equals("/actuator/health") ||
               uri.startsWith("/actuator/");
    }
}