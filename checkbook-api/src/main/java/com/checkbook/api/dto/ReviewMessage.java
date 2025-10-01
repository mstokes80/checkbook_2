package com.checkbook.api.dto;

/**
 * DTO for review message in permission request approval/denial
 */
public class ReviewMessage {

    private String message;

    public ReviewMessage() {}

    public ReviewMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}