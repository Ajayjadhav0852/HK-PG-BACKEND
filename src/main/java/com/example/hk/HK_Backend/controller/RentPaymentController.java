package com.example.hk.HK_Backend.controller;

import com.example.hk.HK_Backend.dto.ApiResponse;
import com.example.hk.HK_Backend.service.EmailService;
import com.example.hk.HK_Backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "Rent Payments", description = "Student rent payment submission and admin confirmation")
@RestController
@RequestMapping("/api/rent")
@RequiredArgsConstructor
public class RentPaymentController {

    private final EmailService     emailService;
    private final FileStorageService fileStorageService;

    @Value("${app.site.url:https://hk-pg-akurdi.vercel.app}")
    private String siteUrl;

    @Value("${app.admin.email:hk.pg.akurdi@gmail.com}")
    private String adminEmail;

    /**
     * POST /api/rent/submit
     * Student submits rent payment with screenshot.
     * Sends email to admin with payment details + "Mark as Received" button.
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> submitPayment(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("studentName")  String studentName,
            @RequestParam("studentEmail") String studentEmail,
            @RequestParam("bedNumber")    String bedNumber,
            @RequestParam("roomNumber")   String roomNumber,
            @RequestParam("roomType")     String roomType,
            @RequestParam("amount")       String amount,
            @RequestParam("month")        String month,
            @RequestParam("screenshot")   MultipartFile screenshot) {

        log.info("Rent payment submitted by {} for bed {} month {}", studentName, bedNumber, month);

        // Upload screenshot to Cloudinary
        String screenshotUrl = null;
        try {
            screenshotUrl = fileStorageService.store(screenshot, "rent-payments");
        } catch (Exception e) {
            log.warn("Screenshot upload failed: {}", e.getMessage());
        }

        // Send email to admin
        emailService.sendRentPaymentToAdmin(
            adminEmail, studentName, studentEmail,
            bedNumber, roomNumber, roomType,
            amount, month, screenshotUrl, siteUrl
        );

        return ResponseEntity.ok(ApiResponse.ok("Payment submitted. Admin has been notified.", null));
    }

    /**
     * POST /api/rent/confirm
     * Admin confirms rent payment received.
     * Sends thank-you email to student.
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPayment(
            @RequestParam("studentEmail") String studentEmail,
            @RequestParam("studentName")  String studentName,
            @RequestParam("bedNumber")    String bedNumber,
            @RequestParam("roomType")     String roomType,
            @RequestParam("amount")       String amount,
            @RequestParam("month")        String month) {

        log.info("Admin confirmed rent payment for {} bed {} month {}", studentName, bedNumber, month);

        emailService.sendRentConfirmationToStudent(
            studentEmail, studentName, bedNumber, roomType, amount, month
        );

        return ResponseEntity.ok(ApiResponse.ok("Confirmation sent to student.", null));
    }
}
