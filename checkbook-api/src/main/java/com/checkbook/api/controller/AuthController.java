package com.checkbook.api.controller;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.User;
import com.checkbook.api.service.AuthenticationService;
import com.checkbook.api.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        ApiResponse response = authenticationService.registerUser(registerRequest);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Authenticate user
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        ApiResponse response = authenticationService.authenticateUser(loginRequest);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Refresh JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        ApiResponse response = authenticationService.refreshToken(refreshTokenRequest.getRefreshToken());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get current user information
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ApiResponse response = authenticationService.getCurrentUser(user.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest updateRequest,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        ApiResponse response = authenticationService.updateUserProfile(user.getUsername(), updateRequest);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Initiate password reset
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest,
            HttpServletRequest request) {

        ApiResponse response = passwordResetService.initiatePasswordReset(forgotPasswordRequest, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest,
            HttpServletRequest request) {

        ApiResponse response = passwordResetService.resetPassword(resetPasswordRequest, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Validate reset token
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse> validateResetToken(@RequestParam String token) {
        ApiResponse response = passwordResetService.validateResetToken(token);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Logout user (placeholder for JWT blacklist implementation)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(Authentication authentication) {
        // For now, just return success
        // In a full implementation, you would add the JWT to a blacklist
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Authentication service is running"));
    }
}