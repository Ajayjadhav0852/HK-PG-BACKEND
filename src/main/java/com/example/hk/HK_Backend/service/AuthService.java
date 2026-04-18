package com.example.hk.HK_Backend.service;

import com.example.hk.HK_Backend.dto.AuthResponse;
import com.example.hk.HK_Backend.dto.LoginRequest;
import com.example.hk.HK_Backend.dto.RegisterRequest;
import com.example.hk.HK_Backend.entity.Role;
import com.example.hk.HK_Backend.entity.User;
import com.example.hk.HK_Backend.exception.BadRequestException;
import com.example.hk.HK_Backend.repository.UserRepository;
import com.example.hk.HK_Backend.security.JwtUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils              jwtUtils;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("This email is already registered. Please login instead.");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .phone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null)
                .active(true)
                .build();

        userRepository.save(user);

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String token = jwtUtils.generateToken((UserDetails) auth.getPrincipal());

        return toAuthResponse(user, token);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        return toAuthResponse(user, token);
    }

    // ── Google OAuth Login ────────────────────────────────────────────────────
    @Transactional
    public AuthResponse googleLogin(String googleToken) {
        try {
            // Verify Google token by calling Google's tokeninfo endpoint
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + googleToken))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BadRequestException("Invalid Google token");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.body());

            String email = json.get("email").asText();
            String name  = json.has("name") ? json.get("name").asText() : email.split("@")[0];
            String picture = json.has("picture") ? json.get("picture").asText() : null;

            // Find or create user
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                        .role(Role.STUDENT)
                        .profilePhotoUrl(picture)
                        .active(true)
                        .build();
                return userRepository.save(newUser);
            });

            // Update profile photo if changed
            if (picture != null && !picture.equals(user.getProfilePhotoUrl())) {
                user.setProfilePhotoUrl(picture);
                userRepository.save(user);
            }

            // Generate JWT
            org.springframework.security.core.userdetails.UserDetails userDetails =
                    org.springframework.security.core.userdetails.User.builder()
                            .username(user.getEmail())
                            .password(user.getPassword())
                            .roles(user.getRole().name())
                            .build();
            String token = jwtUtils.generateToken(userDetails);

            return toAuthResponse(user, token);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Google login failed: " + e.getMessage());
        }
    }

    
    
    @Transactional
    public void changePassword(String userEmail, String currentPassword, String newPassword, String confirmPassword) {
        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        // Find user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Don't allow same password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void updateAdminPassword(String email, String newPassword) {
    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null) {
        user = User.builder()
                .name("Admin")
                .email(email)
                .password(passwordEncoder.encode(newPassword))
                .role(Role.ADMIN)
                .active(true)
                .build();
    } else {
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    userRepository.save(user);
}

    // ── Helper ────────────────────────────────────────────────────────────────
    private AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phone(user.getPhone() != null ? user.getPhone() : "")
                .profilePhotoUrl(user.getProfilePhotoUrl() != null ? user.getProfilePhotoUrl() : "")
                .build();
    }
}