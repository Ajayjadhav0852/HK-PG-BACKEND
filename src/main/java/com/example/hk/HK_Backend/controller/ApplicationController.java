package com.example.hk.HK_Backend.controller;

import com.example.hk.HK_Backend.dto.ApiResponse;
import com.example.hk.HK_Backend.dto.ApplicationRequest;
import com.example.hk.HK_Backend.dto.ApplicationResponse;
import com.example.hk.HK_Backend.exception.BadRequestException;
import com.example.hk.HK_Backend.service.ApplicationService;
import com.example.hk.HK_Backend.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Applications",
     description = "Submit and manage PG admission applications. Submitting is public. Viewing requires JWT.")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * POST /api/applications
     * Accepts multipart/form-data:
     *   - "data"    : JSON string (ApplicationRequest)
     *   - "idProof" : file (optional)
     *   - "photo"   : file (optional)
     */
    @Operation(summary = "Submit a new admission application",
               description = "Accepts multipart/form-data. 'data' part is a JSON string of ApplicationRequest.",
               security = {})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Application submitted successfully",
            content = @Content(schema = @Schema(implementation = ApplicationResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Validation error or room type not found")
    })
    @SecurityRequirements
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ApplicationResponse>> submit(
            @Parameter(description = "Application JSON string", required = true)
            @RequestPart("data") String dataJson,
            @Parameter(description = "ID proof file (JPG/PNG/PDF, max 5MB)")
            @RequestPart(value = "idProof", required = false) MultipartFile idProof,
            @Parameter(description = "Profile photo (JPG/PNG, max 5MB)")
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @AuthenticationPrincipal UserDetails userDetails) throws Exception {

        // Deserialize JSON string → ApplicationRequest
        ApplicationRequest request;
        try {
            request = objectMapper.readValue(dataJson, ApplicationRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Invalid request data: " + e.getMessage());
        }

        // Bean validation
        Set<ConstraintViolation<ApplicationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new BadRequestException(msg);
        }

        String email = userDetails != null ? userDetails.getUsername() : null;
        ApplicationResponse response = applicationService.submitApplication(request, email);

        if (idProof != null && !idProof.isEmpty()) {
            String url = fileStorageService.store(idProof, "id-proofs");
            applicationService.updateIdProofUrl(response.getId(), url);
            response.setIdProofUrl(url);
        }
        if (photo != null && !photo.isEmpty()) {
            String url = fileStorageService.store(photo, "photos");
            applicationService.updateProfilePhotoUrl(response.getId(), url);
            response.setProfilePhotoUrl(url);
        }

        return ResponseEntity.ok(ApiResponse.ok("Application submitted successfully", response));
    }

    @Operation(summary = "Get my applications",
               description = "Returns all applications for the logged-in student.",
               security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of applications"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> myApplications(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getMyApplications(userDetails.getUsername())));
    }

    @Operation(summary = "Get application by ID",
               security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Application found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getById(
            @Parameter(description = "Application ID", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getById(id)));
    }
}
