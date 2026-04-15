package com.example.hk.HK_Backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight health-check endpoint.
 * Used by the frontend keep-alive ping to prevent Render free-tier cold starts.
 * No auth required — returns 200 OK immediately.
 */
@Tag(name = "Health", description = "Keep-alive / health check")
@RestController
@SecurityRequirements
public class HealthController {

    @Operation(summary = "Health check — returns 200 OK, used to keep Render awake")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
