package com.checkbook.api.service;

import com.checkbook.api.dto.ApiResponse;
import com.checkbook.api.dto.ForgotPasswordRequest;
import com.checkbook.api.dto.ResetPasswordRequest;
import com.checkbook.api.entity.PasswordResetToken;
import com.checkbook.api.entity.User;
import com.checkbook.api.repository.PasswordResetTokenRepository;
import com.checkbook.api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_HOURS = 1;
    private static final int MAX_REQUESTS_PER_HOUR_PER_USER = 3;
    private static final int MAX_REQUESTS_PER_HOUR_PER_IP = 10;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    /**
     * Initiate password reset process
     */
    public ApiResponse initiatePasswordReset(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        try {
            String email = request.getEmail().toLowerCase().trim();
            String clientIp = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            // Check rate limiting by IP
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            Long ipRequestCount = passwordResetTokenRepository.countRecentTokensFromIp(clientIp, oneHourAgo);

            if (ipRequestCount >= MAX_REQUESTS_PER_HOUR_PER_IP) {
                logger.warn("Too many password reset requests from IP: {}", clientIp);
                return ApiResponse.error("Too many password reset requests. Please try again later.");
            }

            // Find user by email
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

            if (userOpt.isEmpty()) {
                // For security, don't reveal if email exists
                logger.info("Password reset requested for non-existent email: {}", email);
                return ApiResponse.success("If the email exists, a password reset link has been sent.");
            }

            User user = userOpt.get();

            // Check if account is enabled
            if (!user.getEnabled()) {
                logger.warn("Password reset requested for disabled account: {}", user.getUsername());
                return ApiResponse.success("If the email exists, a password reset link has been sent.");
            }

            // Check rate limiting by user
            Long userRequestCount = passwordResetTokenRepository.countRecentTokensForUser(user.getId(), oneHourAgo);

            if (userRequestCount >= MAX_REQUESTS_PER_HOUR_PER_USER) {
                logger.warn("Too many password reset requests for user: {}", user.getUsername());
                return ApiResponse.error("Too many password reset requests. Please try again later.");
            }

            // Invalidate existing tokens for this user
            passwordResetTokenRepository.markAllUserTokensAsUsed(user.getId(), LocalDateTime.now());

            // Generate secure token
            String token = generateSecureToken();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

            // Create new reset token
            PasswordResetToken resetToken = new PasswordResetToken(
                    user.getId(), token, expiresAt, clientIp, userAgent);

            passwordResetTokenRepository.save(resetToken);

            // Send email (async)
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);

            logger.info("Password reset token generated for user: {}", user.getUsername());

            return ApiResponse.success("If the email exists, a password reset link has been sent.");

        } catch (Exception e) {
            logger.error("Error initiating password reset: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to process password reset request. Please try again.");
        }
    }

    /**
     * Reset password using token
     */
    public ApiResponse resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        try {
            String token = request.getToken();
            String newPassword = request.getNewPassword();
            String clientIp = getClientIpAddress(httpRequest);

            // Find valid token
            Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository
                    .findValidToken(token, LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                logger.warn("Invalid or expired password reset token used from IP: {}", clientIp);
                return ApiResponse.error("Invalid or expired reset token.");
            }

            PasswordResetToken resetToken = tokenOpt.get();

            // Find user
            Optional<User> userOpt = userRepository.findById(resetToken.getUserId());

            if (userOpt.isEmpty()) {
                logger.error("User not found for valid reset token: {}", resetToken.getUserId());
                return ApiResponse.error("User not found.");
            }

            User user = userOpt.get();

            // Check if account is still enabled
            if (!user.getEnabled()) {
                logger.warn("Password reset attempted for disabled account: {}", user.getUsername());
                return ApiResponse.error("Account is disabled.");
            }

            // Update password
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setUpdatedBy("PASSWORD_RESET");

            // If account was locked, unlock it
            if (user.getAccountLocked()) {
                user.unlockAccount();
                logger.info("Account unlocked during password reset: {}", user.getUsername());
            }

            userRepository.save(user);

            // Mark token as used
            resetToken.markAsUsed();
            passwordResetTokenRepository.save(resetToken);

            // Invalidate all other tokens for this user
            passwordResetTokenRepository.markAllUserTokensAsUsed(user.getId(), LocalDateTime.now());

            logger.info("Password reset successfully for user: {}", user.getUsername());

            return ApiResponse.success("Password has been reset successfully. You can now log in with your new password.");

        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to reset password. Please try again.");
        }
    }

    /**
     * Validate reset token
     */
    public ApiResponse validateResetToken(String token) {
        try {
            Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository
                    .findValidToken(token, LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                return ApiResponse.error("Invalid or expired reset token.");
            }

            PasswordResetToken resetToken = tokenOpt.get();

            // Check if user still exists and is enabled
            Optional<User> userOpt = userRepository.findById(resetToken.getUserId());

            if (userOpt.isEmpty() || !userOpt.get().getEnabled()) {
                return ApiResponse.error("Invalid reset token.");
            }

            return ApiResponse.success("Reset token is valid.");

        } catch (Exception e) {
            logger.error("Error validating reset token: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to validate reset token.");
        }
    }

    /**
     * Generate cryptographically secure token
     */
    private String generateSecureToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] token = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Clean up expired tokens (runs daily at midnight)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(1); // Keep for 1 day after expiry
            passwordResetTokenRepository.deleteExpiredTokens(cutoffTime);
            logger.info("Cleaned up expired password reset tokens");
        } catch (Exception e) {
            logger.error("Error cleaning up expired tokens: {}", e.getMessage(), e);
        }
    }
}