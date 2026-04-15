package com.example.hk.HK_Backend.controller;

import com.example.hk.HK_Backend.dto.*;
import com.example.hk.HK_Backend.service.AdminService;
import com.example.hk.HK_Backend.service.ApplicationService;
import com.example.hk.HK_Backend.service.AuthService;
import com.example.hk.HK_Backend.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
    name = "Admin",
    description = "Admin-only endpoints for managing the PG. " +
                  "All endpoints require a JWT token with **ADMIN** role. " +
                  "Login with `admin@hkpg.com` / `admin123` to get the token."
)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final ApplicationService applicationService;
    private final RoomService roomService;
    private final AuthService authService;

    @Operation(
        summary = "Get dashboard statistics",
        description = "Returns a summary of the entire PG:\n\n" +
                      "- Total registered students\n" +
                      "- Pending / Confirmed / Rejected application counts\n" +
                      "- Total beds, occupied beds, vacant beds across all room types"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Dashboard stats returned",
            content = @Content(schema = @Schema(implementation = DashboardStatsDto.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Access denied — Admin role required"
        )
    })
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDashboardStats()));
    }

    @Operation(
        summary = "Get all applications",
        description = "Returns every admission application in the system, sorted by newest first. " +
                      "Includes applicant details, assigned room, payment info, and current status."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "All applications returned",
            content = @Content(schema = @Schema(implementation = ApplicationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Access denied — Admin role required"
        )
    })
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getAllApplications() {
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getAllApplications()));
    }

    @Operation(
        summary = "Update application status",
        description = "Changes the status of an application to **CONFIRMED**, **PENDING**, or **REJECTED**.\n\n" +
                      "**Side effects:**\n" +
                      "- `CONFIRMED` → increments `occupiedBeds` on the assigned room\n" +
                      "- `REJECTED` (from CONFIRMED) → decrements `occupiedBeds` back\n\n" +
                      "An optional `adminNotes` field can be used to record a reason."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Status updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "No vacant beds available to confirm"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Application not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Access denied — Admin role required"
        )
    })
    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(
            @Parameter(description = "Application ID", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "New status and optional admin notes",
                content = @Content(examples = {
                    @ExampleObject(name = "Confirm",  value = """
                        { "status": "CONFIRMED", "adminNotes": "Room R201 assigned" }
                        """),
                    @ExampleObject(name = "Reject",   value = """
                        { "status": "REJECTED",  "adminNotes": "Incomplete documents" }
                        """),
                    @ExampleObject(name = "Pending",  value = """
                        { "status": "PENDING",   "adminNotes": "Awaiting ID proof" }
                        """)
                })
            )
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Status updated", applicationService.updateStatus(id, request)));
    }

    @Operation(summary = "Update room type price and/or image (Admin only)",
               description = "Update monthlyPrice, securityDeposit, imageUrl, or description of a room type.\n\n" +
                             "For imageUrl, you can paste a **Google Drive share link** — it will be auto-converted to a direct image URL.\n\n" +
                             "Google Drive share link format: `https://drive.google.com/file/d/FILE_ID/view?usp=sharing`")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room type updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Room type not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin only")
    })
    @PatchMapping("/room-types/{slug}")
    public ResponseEntity<ApiResponse<RoomTypeDto>> updateRoomType(
            @Parameter(description = "Room type slug", example = "2-sharing")
            @PathVariable String slug,
            @Valid @RequestBody UpdateRoomTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Room type updated", roomService.updateRoomType(slug, request)));
    }

    @Operation(summary = "Delete an application",
               description = "Permanently deletes an application. " +
                             "If the application was PENDING or CONFIRMED, the reserved bed is freed first.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Application deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Application not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin only")
    })
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApplication(
            @Parameter(description = "Application ID", example = "1")
            @PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.ok(ApiResponse.ok("Application deleted successfully", null));
    }

    @Operation(
        summary = "Change admin password",
        description = "Allows the admin to securely change their password. " +
                      "Requires current password verification for security."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Password changed successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid current password or password validation failed"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Access denied — Admin role required"
        )
    })
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        
        String adminEmail = authentication.getName();
        authService.changePassword(
            adminEmail, 
            request.getCurrentPassword(), 
            request.getNewPassword(), 
            request.getConfirmPassword()
        );
        
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }

}
