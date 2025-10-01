package com.checkbook.api.service;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.User;
import com.checkbook.api.repository.UserRepository;
import com.checkbook.api.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Register a new user
     */
    public ApiResponse registerUser(RegisterRequest registerRequest) {
        try {
            // Check if username already exists
            if (userRepository.existsByUsernameIgnoreCase(registerRequest.getUsername())) {
                return ApiResponse.error("Username is already taken!");
            }

            // Check if email already exists
            if (userRepository.existsByEmailIgnoreCase(registerRequest.getEmail())) {
                return ApiResponse.error("Email is already in use!");
            }

            // Create new user
            User user = new User(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    passwordEncoder.encode(registerRequest.getPassword()),
                    registerRequest.getFirstName(),
                    registerRequest.getLastName()
            );

            // Set audit fields
            user.setCreatedBy("SYSTEM");
            user.setUpdatedBy("SYSTEM");

            // Save user
            User savedUser = userRepository.save(user);

            logger.info("New user registered successfully: {}", savedUser.getUsername());

            return ApiResponse.success("User registered successfully! You can now log in.",
                    new UserInfo(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(),
                            savedUser.getFullName(), savedUser.getEmailVerified()));

        } catch (Exception e) {
            logger.error("Error during user registration: {}", e.getMessage(), e);
            return ApiResponse.error("Registration failed. Please try again.");
        }
    }

    /**
     * Authenticate user and generate JWT
     */
    public ApiResponse authenticateUser(LoginRequest loginRequest) {
        try {
            // Find user first to check account status
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail());

            if (userOpt.isEmpty()) {
                logger.warn("Login attempt with non-existent username/email: {}", loginRequest.getUsernameOrEmail());
                return ApiResponse.error("Invalid username/email or password");
            }

            User user = userOpt.get();

            // Check if account is locked
            if (user.getAccountLocked()) {
                logger.warn("Login attempt on locked account: {}", user.getUsername());
                return ApiResponse.error("Account is locked. Please contact support.");
            }

            // Check if account is enabled
            if (!user.getEnabled()) {
                logger.warn("Login attempt on disabled account: {}", user.getUsername());
                return ApiResponse.error("Account is disabled. Please contact support.");
            }

            // Attempt authentication
            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getUsernameOrEmail(),
                                loginRequest.getPassword())
                );
            } catch (AuthenticationException e) {
                // Handle failed authentication
                handleFailedLogin(user);
                throw new BadCredentialsException("Invalid username/email or password");
            }

            // Authentication successful - reset failed attempts
            handleSuccessfulLogin(user);

            // Generate JWT tokens
            String jwt = jwtUtils.generateJwtToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(user);

            logger.info("User authenticated successfully: {}", user.getUsername());

            return ApiResponse.success("Login successful",
                    new JwtResponse(jwt, refreshToken, user.getId(), user.getUsername(),
                            user.getEmail(), user.getFullName(), user.getEmailVerified()));

        } catch (BadCredentialsException e) {
            logger.warn("Invalid login attempt for: {}", loginRequest.getUsernameOrEmail());
            return ApiResponse.error("Invalid username/email or password");
        } catch (Exception e) {
            logger.error("Error during authentication: {}", e.getMessage(), e);
            return ApiResponse.error("Authentication failed. Please try again.");
        }
    }

    /**
     * Refresh JWT token
     */
    public ApiResponse refreshToken(String refreshToken) {
        try {
            if (!jwtUtils.validateJwtToken(refreshToken)) {
                return ApiResponse.error("Invalid refresh token");
            }

            String tokenType = jwtUtils.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                return ApiResponse.error("Invalid token type");
            }

            String username = jwtUtils.getUsernameFromJwtToken(refreshToken);
            Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);

            if (userOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            User user = userOpt.get();

            // Check if user is still enabled and not locked
            if (!user.getEnabled() || user.getAccountLocked()) {
                return ApiResponse.error("Account is not active");
            }

            // Generate new access token
            String newAccessToken = jwtUtils.generateTokenForUser(user);
            String newRefreshToken = jwtUtils.generateRefreshToken(user);

            logger.info("Token refreshed for user: {}", user.getUsername());

            return ApiResponse.success("Token refreshed successfully",
                    new JwtResponse(newAccessToken, newRefreshToken, user.getId(), user.getUsername(),
                            user.getEmail(), user.getFullName(), user.getEmailVerified()));

        } catch (Exception e) {
            logger.error("Error refreshing token: {}", e.getMessage(), e);
            return ApiResponse.error("Token refresh failed");
        }
    }

    /**
     * Get current user information
     */
    public ApiResponse getCurrentUser(String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);

            if (userOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            User user = userOpt.get();
            UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), user.getEmail(),
                    user.getFullName(), user.getEmailVerified());

            return ApiResponse.success("User information retrieved", userInfo);

        } catch (Exception e) {
            logger.error("Error getting current user: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get user information");
        }
    }

    /**
     * Handle failed login attempt
     */
    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();

        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.lockAccount();
            logger.warn("Account locked due to too many failed attempts: {}", user.getUsername());
        }

        userRepository.save(user);
    }

    /**
     * Handle successful login
     */
    private void handleSuccessfulLogin(User user) {
        user.resetFailedLoginAttempts();
        userRepository.save(user);
    }

    /**
     * Update user profile
     */
    public ApiResponse updateUserProfile(String username, UpdateProfileRequest updateRequest) {
        try {
            Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);

            if (userOpt.isEmpty()) {
                return ApiResponse.error("User not found");
            }

            User user = userOpt.get();

            // Update fields if provided
            if (updateRequest.getFirstName() != null && !updateRequest.getFirstName().trim().isEmpty()) {
                user.setFirstName(updateRequest.getFirstName().trim());
            }

            if (updateRequest.getLastName() != null && !updateRequest.getLastName().trim().isEmpty()) {
                user.setLastName(updateRequest.getLastName().trim());
            }

            if (updateRequest.getEmail() != null && !updateRequest.getEmail().trim().isEmpty()) {
                // Check if new email already exists
                if (!user.getEmail().equalsIgnoreCase(updateRequest.getEmail()) &&
                        userRepository.existsByEmailIgnoreCase(updateRequest.getEmail())) {
                    return ApiResponse.error("Email is already in use");
                }
                user.setEmail(updateRequest.getEmail().trim());
                user.setEmailVerified(false); // Re-verification required for new email
            }

            user.setUpdatedBy(username);
            User updatedUser = userRepository.save(user);

            logger.info("User profile updated: {}", user.getUsername());

            return ApiResponse.success("Profile updated successfully",
                    new UserInfo(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getEmail(),
                            updatedUser.getFullName(), updatedUser.getEmailVerified()));

        } catch (Exception e) {
            logger.error("Error updating user profile: {}", e.getMessage(), e);
            return ApiResponse.error("Profile update failed");
        }
    }

    // Inner class for user information response
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private boolean emailVerified;

        public UserInfo(Long id, String username, String email, String fullName, boolean emailVerified) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.emailVerified = emailVerified;
        }

        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public boolean isEmailVerified() { return emailVerified; }
    }
}