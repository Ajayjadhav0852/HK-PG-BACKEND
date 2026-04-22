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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils              jwtUtils;
    private final EmailService          emailService;

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
    public AuthResponse googleLogin(String accessToken) {
        try {
            // Use access_token to get user info from Google userinfo endpoint
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BadRequestException("Could not verify Google account. Please try again.");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.body());

            String email   = json.has("email")   ? json.get("email").asText()   : null;
            String name    = json.has("name")    ? json.get("name").asText()    : null;
            String picture = json.has("picture") ? json.get("picture").asText() : null;

            if (email == null || email.isBlank()) {
                throw new BadRequestException("Google account does not have an email address.");
            }

            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            // Find or create user
            final String finalName = name;
            final String finalPicture = picture;
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = User.builder()
                        .name(finalName)
                        .email(email)
                        .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                        .role(Role.STUDENT)
                        .profilePhotoUrl(finalPicture)
                        .active(true)
                        .build();
                return userRepository.save(newUser);
            });

            // Update profile photo if changed
            if (picture != null && !picture.equals(user.getProfilePhotoUrl())) {
                user.setProfilePhotoUrl(picture);
                userRepository.save(user);
            }

            // Generate JWT using UserDetails
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
            log.error("Google login error: {}", e.getMessage());
            throw new BadRequestException("Google login failed. Please try again.");
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

    // ── Forgot Password — sends reset email ───────────────────────────────────
    public boolean sendPasswordResetEmail(String email) {
        final boolean[] emailSent = {false};
        // Find user — if not found, silently return (don't reveal existence)
        userRepository.findByEmail(email).ifPresent(user -> {
            // Generate a temporary password
            String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
            user.setPassword(passwordEncoder.encode(tempPassword));
            userRepository.save(user);

            log.info("Password reset requested for email: {}", email);

            // Build content WITHOUT calling wrap() — pass raw HTML directly
            // wrap() uses String.formatted() which breaks if content has % chars
            String htmlBody = "<!DOCTYPE html><html><head><meta charset='UTF-8'/></head><body style='margin:0;padding:0;background:#f4f4f8;font-family:Segoe UI,Arial,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f4f8;padding:30px 0;'><tr><td align='center'>"
                + "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;'>"
                // HD Logo header
                + "<tr><td style='background:#1a1a2e;border-radius:16px 16px 0 0;padding:0;text-align:center;'>"
                + "<img src='https://res.cloudinary.com/dqveipmse/image/upload/v1734970827/hkpg-email-header_lfqwxe.png' "
                + "alt='HK PG - Boys PG Accommodation' "
                + "style='width:100%;max-width:600px;height:auto;display:block;border-radius:16px 16px 0 0;'/>"
                + "</td></tr>"
                // Content
                + "<tr><td style='background:#fff;padding:32px 36px;'>"
                + "<h2 style='margin:0 0 8px;color:#1a1a2e;font-size:20px;font-weight:800;'>&#128273; Password Reset</h2>"
                + "<p style='margin:0 0 20px;color:#6b7280;font-size:14px;'>You requested a password reset for your HK PG account.</p>"
                + "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:20px;margin-bottom:16px;text-align:center;'>"
                + "<p style='margin:0 0 8px;color:#6b7280;font-size:13px;'>Your temporary password:</p>"
                + "<p style='margin:0;font-family:monospace;font-size:26px;font-weight:900;color:#c026d3;letter-spacing:4px;'>" + tempPassword + "</p>"
                + "</div>"
                + "<div style='background:#fff3cd;border:1px solid #ffc107;border-radius:10px;padding:14px;margin-bottom:16px;'>"
                + "<p style='margin:0;color:#856404;font-size:13px;font-weight:600;'>&#9888;&#65039; Login with this temporary password and change it immediately from your dashboard.</p>"
                + "</div>"
                + "<p style='color:#6b7280;font-size:12px;'>If you did not request this, contact us at <a href='tel:9579828996' style='color:#c026d3;'>9579828996</a></p>"
                + "<hr style='border:none;border-top:1px solid #f1f5f9;margin:20px 0;'/>"
                + "<p style='margin:0;color:#374151;font-size:13px;'>Thanks &amp; Regards,<br/><strong style='color:#c026d3;'>HK PG MANAGEMENT</strong></p>"
                + "</td></tr>"
                // Footer with icon-only social links
                + "<tr><td style='background:#1a1a2e;border-radius:0 0 16px 16px;padding:28px 36px;text-align:center;'>"
                + "<p style='margin:0 0 6px;color:rgba(255,255,255,0.9);font-size:14px;font-weight:700;'>HK PG Akurdi</p>"
                + "<p style='margin:0 0 16px;color:rgba(255,255,255,0.6);font-size:12px;'>&#128205; Near Gurudwara, Akurdi Railway Station, Pune &#8211; 411035</p>"
                + "<div style='margin:0 0 16px;'>"
                + "<a href='https://wa.me/919579828996' style='display:inline-block;margin:0 6px;'>"
                + "<img src='https://upload.wikimedia.org/wikipedia/commons/6/6b/WhatsApp.svg' alt='WhatsApp' width='36' height='36' style='border-radius:8px;'/>"
                + "</a>"
                + "<a href='https://www.instagram.com/hkpg.akurdi' style='display:inline-block;margin:0 6px;'>"
                + "<img src='https://upload.wikimedia.org/wikipedia/commons/a/a5/Instagram_icon.png' alt='Instagram' width='36' height='36' style='border-radius:8px;'/>"
                + "</a>"
                + "<a href='https://hk-pg-akurdi.vercel.app' style='display:inline-block;margin:0 6px;'>"
                + "<img src='https://upload.wikimedia.org/wikipedia/commons/8/8e/Antu_internet-web-browser.svg' alt='Website' width='36' height='36' style='border-radius:8px;'/>"
                + "</a>"
                + "</div>"
                + "<p style='margin:0;color:rgba(255,255,255,0.4);font-size:11px;'>&#169; 2026 HK PG Akurdi. All rights reserved.</p>"
                + "</td></tr>"
                + "</table></td></tr></table>"
                + "</body></html>";

            try {
                emailService.sendPasswordResetEmailDirect(email, htmlBody);
                emailSent[0] = true;
                log.info("Password reset email dispatched for: {}", email);
            } catch (Exception e) {
                log.error("Failed to send password reset email to {}: {}", email, e.getMessage(), e);
            }
        });
        return emailSent[0];
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