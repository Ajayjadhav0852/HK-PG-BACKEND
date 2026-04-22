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
                // Inline styled header — no external images
                + "<tr><td style='background:linear-gradient(135deg,#0f0c29,#1a1a2e,#16213e);border-radius:16px 16px 0 0;padding:32px 24px 24px;text-align:center;'>"
                + "<div style='display:inline-block;border:2px solid #b8860b;border-radius:12px;padding:10px 28px;margin-bottom:10px;background:rgba(0,0,0,0.3);'>"
                + "<span style='font-size:42px;font-weight:900;letter-spacing:2px;'>"
                + "<span style='color:#c8a84b;text-shadow:0 0 20px rgba(200,168,75,0.5);'>HK</span>"
                + "<span style='color:#a8a8b8;text-shadow:0 0 20px rgba(168,168,184,0.5);'>PG</span>"
                + "</span></div>"
                + "<div style='background:#b8860b;border-radius:6px;padding:5px 20px;display:inline-block;'>"
                + "<span style='color:#fff;font-size:12px;font-weight:800;letter-spacing:3px;text-transform:uppercase;'>Boys PG Accommodation</span>"
                + "</div>"
                + "<p style='margin:10px 0 0;color:rgba(255,255,255,0.6);font-size:12px;'>Near Akurdi Railway Station, Pune</p>"
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
                + "<a href='https://wa.me/919579828996' style='display:inline-block;margin:0 8px;text-decoration:none;'>"
                + "<div style='width:40px;height:40px;background:#25d366;border-radius:10px;display:inline-flex;align-items:center;justify-content:center;'>"
                + "<svg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='white'><path d='M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z'/><path d='M12 0C5.373 0 0 5.373 0 12c0 2.123.554 4.118 1.528 5.855L.057 23.882a.5.5 0 0 0 .61.61l6.086-1.464A11.945 11.945 0 0 0 12 24c6.627 0 12-5.373 12-12S18.627 0 12 0zm0 21.818a9.818 9.818 0 0 1-5.006-1.373l-.36-.214-3.713.894.924-3.638-.234-.374A9.818 9.818 0 1 1 12 21.818z'/></svg>"
                + "</div></a>"
                + "<a href='https://www.instagram.com/hkpg.akurdi' style='display:inline-block;margin:0 8px;text-decoration:none;'>"
                + "<div style='width:40px;height:40px;background:linear-gradient(45deg,#f09433,#e6683c,#dc2743,#cc2366,#bc1888);border-radius:10px;display:inline-flex;align-items:center;justify-content:center;'>"
                + "<svg xmlns='http://www.w3.org/2000/svg' width='22' height='22' viewBox='0 0 24 24' fill='white'><path d='M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z'/></svg>"
                + "</div></a>"
                + "<a href='https://hk-pg-akurdi.vercel.app' style='display:inline-block;margin:0 8px;text-decoration:none;'>"
                + "<div style='width:40px;height:40px;background:linear-gradient(135deg,#d63384,#c026d3);border-radius:10px;display:inline-flex;align-items:center;justify-content:center;'>"
                + "<svg xmlns='http://www.w3.org/2000/svg' width='22' height='22' viewBox='0 0 24 24' fill='white'><path d='M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0zm-1 21.945A10.01 10.01 0 0 1 2.055 13H5.1a14.93 14.93 0 0 0 1.58 5.698A10.044 10.044 0 0 1 11 21.945zm0-19.89A10.044 10.044 0 0 1 6.68 5.302 14.93 14.93 0 0 0 5.1 11H2.055A10.01 10.01 0 0 1 11 2.055zM13 2.055A10.01 10.01 0 0 1 21.945 11H18.9a14.93 14.93 0 0 0-1.58-5.698A10.044 10.044 0 0 1 13 2.055zm0 19.89a10.044 10.044 0 0 1-4.32-3.247A14.93 14.93 0 0 0 10.9 13h2.2a14.93 14.93 0 0 0 1.58 5.698A10.044 10.044 0 0 1 13 21.945zM10.9 11a12.93 12.93 0 0 1 1.1-4.9 12.93 12.93 0 0 1 1.1 4.9h-2.2zm0 2h2.2a12.93 12.93 0 0 1-1.1 4.9A12.93 12.93 0 0 1 10.9 13zm6 0h2.945A10.01 10.01 0 0 1 13 21.945a10.044 10.044 0 0 1 4.32-3.247A14.93 14.93 0 0 0 18.9 13zm0-2a14.93 14.93 0 0 0-1.58-5.698A10.044 10.044 0 0 1 21.945 11H18.9z'/></svg>"
                + "</div></a>"
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