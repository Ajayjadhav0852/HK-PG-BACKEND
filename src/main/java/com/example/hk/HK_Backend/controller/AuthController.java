package com.example.hk.HK_Backend.controller;

import com.example.hk.HK_Backend.dto.ApiResponse;
import com.example.hk.HK_Backend.dto.AuthResponse;
import com.example.hk.HK_Backend.dto.LoginRequest;
import com.example.hk.HK_Backend.dto.RegisterRequest;
import com.example.hk.HK_Backend.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Authentication",
    description = "Register a new student account or login to get a JWT token."
)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request)));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @RequestBody java.util.Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            throw new com.example.hk.HK_Backend.exception.BadRequestException("Google token is required");
        }
        return ResponseEntity.ok(ApiResponse.ok("Google login successful", authService.googleLogin(token)));
    }

    }