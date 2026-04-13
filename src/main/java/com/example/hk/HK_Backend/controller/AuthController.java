package com.example.hk.HK_Backend.controller;

import com.example.hk.HK_Backend.dto.ApiResponse;
import com.example.hk.HK_Backend.dto.AuthResponse;
import com.example.hk.HK_Backend.dto.LoginRequest;
import com.example.hk.HK_Backend.dto.RegisterRequest;
import com.example.hk.HK_Backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Authentication",
    description = "Register a new student account or login to get a JWT token. " +
                  "Use the returned token in the **Authorize 🔒** button above."
)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements   // No auth required for these endpoints
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Register a new student account",
        description = "Creates a new student account. Returns a JWT token on success. " +
                      "The role is automatically set to **STUDENT**."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Registration successful — JWT token returned",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Email already registered or validation failed"
        )
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Student registration details",
                content = @Content(examples = @ExampleObject(value = """
                    {
                      "name": "Rahul Sharma",
                      "email": "rahul@example.com",
                      "password": "rahul123"
                    }
                    """))
            )
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", authService.register(request)));
    }

    @Operation(
        summary = "Login and get JWT token",
        description = "Authenticates the user and returns a JWT token valid for **24 hours**. " +
                      "Use the token in the Authorization header as `Bearer <token>`."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Login successful — JWT token returned",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Invalid email or password"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Login credentials",
                content = @Content(examples = @ExampleObject(value = """
                    {
                      "email": "user@example.com",
                      "password": "yourpassword"
                    }
                    """))
            )
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request)));
    }
}
