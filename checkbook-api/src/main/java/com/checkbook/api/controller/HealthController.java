package com.checkbook.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check database connectivity
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    response.put("status", "UP");
                    response.put("database", "UP");
                } else {
                    response.put("status", "DOWN");
                    response.put("database", "DOWN");
                }
            }
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("database", "DOWN");
            response.put("error", e.getMessage());
        }

        response.put("service", "checkbook-api");
        response.put("timestamp", System.currentTimeMillis());

        String status = (String) response.get("status");
        if ("UP".equals(status)) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }
}