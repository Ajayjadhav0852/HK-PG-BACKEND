package com.example.hk.HK_Backend.controller;

import com.example.hk.HK_Backend.dto.ApiResponse;
import com.example.hk.HK_Backend.dto.UpdateProfileRequest;
import com.example.hk.HK_Backend.entity.User;
import com.example.hk.HK_Backend.exception.ResourceNotFoundException;
import com.example.hk.HK_Backend.repository.UserRepository;
import com.example.hk.HK_Backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Users", description = "User profile management")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserRepository    userRepository;
    private final FileStorageService fileStorageService;

    // ── GET /api/users/me ─────────────────────────────────────────────────────
    @Operation(summary = "Get current user profile",
               description = "Returns id, name, email, role, phone, profilePhotoUrl, createdAt")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = findUser(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(toProfileMap(user)));
    }

    // ── PUT /api/users/me ─────────────────────────────────────────────────────
    @Operation(summary = "Update name and/or phone number",
               description = "Updates the current user's name and/or phone. Both fields are optional.")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        User user = findUser(userDetails);

        if (StringUtils.hasText(request.getName())) {
            user.setName(request.getName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        }

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", toProfileMap(user)));
    }

    // ── POST /api/users/me/photo ──────────────────────────────────────────────
    @Operation(summary = "Upload profile photo",
               description = "Uploads a profile photo (JPG/PNG, max 5MB) and saves the URL to the user record.")
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadPhoto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("photo") MultipartFile photo) {

        User user = findUser(userDetails);

        String url = fileStorageService.store(photo, "profile-photos");
        user.setProfilePhotoUrl(url);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Profile photo updated", toProfileMap(user)));
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private User findUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Map<String, Object> toProfileMap(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              user.getId());
        m.put("name",            user.getName());
        m.put("email",           user.getEmail());
        m.put("role",            user.getRole().name());
        m.put("phone",           user.getPhone() != null ? user.getPhone() : "");
        m.put("profilePhotoUrl", user.getProfilePhotoUrl() != null ? user.getProfilePhotoUrl() : "");
        m.put("createdAt",       user.getCreatedAt());
        return m;
    }
}
