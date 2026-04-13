package com.example.hk.HK_Backend.dto;

import com.example.hk.HK_Backend.entity.GuardianRelation;
import com.example.hk.HK_Backend.entity.IdProofType;
import com.example.hk.HK_Backend.entity.PaymentMode;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ApplicationRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile must be exactly 10 digits")
    private String mobile;

    private String alternateMobile;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Occupation is required")
    private String occupation;

    @NotBlank(message = "College/Company name is required")
    private String institutionName;

    @NotBlank(message = "Course/Job role is required")
    private String courseOrRole;

    @NotBlank(message = "Guardian name is required")
    private String guardianName;

    @NotBlank(message = "Guardian contact is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Guardian contact must be 10 digits")
    private String guardianContact;

    // Nullable — empty string from frontend becomes null via JacksonConfig
    private GuardianRelation guardianRelation;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 month")
    @Max(value = 12, message = "Duration cannot exceed 12 months")
    private Integer durationMonths;

    @NotBlank(message = "Room type is required")
    private String roomTypeSlug;

    /** Optional: specific room number student wants (e.g. "R3").
     *  If provided, the system will try to assign this exact room.
     *  If not provided or room is full, auto-assigns the first vacant room. */
    private String preferredRoomNumber;

    /** The specific bed number the student selected (e.g. 12 for R5 bed 1) */
    private Integer selectedBedNumber;

    private BigDecimal depositAmount;

    // Nullable — empty string from frontend becomes null via JacksonConfig
    private PaymentMode paymentMode;

    private String transactionId;

    // Nullable — empty string from frontend becomes null via JacksonConfig
    private IdProofType idProofType;
}
