package com.checkbook.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Value("${app.name:Checkbook Balance}")
    private String appName;

    @Value("${spring.mail.username:noreply@checkbook.local}")
    private String fromEmail;

    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(appName + " - Password Reset Request");

            String resetUrl = generateResetUrl(resetToken);
            String emailBody = createPasswordResetEmailBody(fullName, resetUrl, resetToken);

            message.setText(emailBody);

            emailSender.send(message);

            logger.info("Password reset email sent to: {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Send welcome email for new registrations
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to " + appName);

            String emailBody = createWelcomeEmailBody(fullName);
            message.setText(emailBody);

            emailSender.send(message);

            logger.info("Welcome email sent to: {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Generate password reset URL
     */
    private String generateResetUrl(String token) {
        // In production, this would be your frontend URL
        // For development, you can use localhost
        return "http://localhost:3000/reset-password?token=" + token;
    }

    /**
     * Create password reset email body
     */
    private String createPasswordResetEmailBody(String fullName, String resetUrl, String token) {
        return String.format(
                "Hello %s,\n\n" +
                "You have requested to reset your password for your %s account.\n\n" +
                "Please click the following link to reset your password:\n%s\n\n" +
                "Alternatively, you can use this reset token: %s\n\n" +
                "This link will expire in 1 hour for security reasons.\n\n" +
                "If you did not request this password reset, please ignore this email. Your password will remain unchanged.\n\n" +
                "Best regards,\n" +
                "The %s Team\n\n" +
                "---\n" +
                "This is an automated message, please do not reply to this email.",
                fullName, appName, resetUrl, token, appName
        );
    }

    /**
     * Create welcome email body
     */
    private String createWelcomeEmailBody(String fullName) {
        return String.format(
                "Hello %s,\n\n" +
                "Welcome to %s!\n\n" +
                "Your account has been successfully created. You can now log in and start managing your checkbook balance.\n\n" +
                "If you have any questions or need assistance, please don't hesitate to contact us.\n\n" +
                "Best regards,\n" +
                "The %s Team\n\n" +
                "---\n" +
                "This is an automated message, please do not reply to this email.",
                fullName, appName, appName
        );
    }
}