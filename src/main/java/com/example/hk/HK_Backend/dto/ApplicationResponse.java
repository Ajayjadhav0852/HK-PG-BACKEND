package com.example.hk.HK_Backend.dto;

import com.example.hk.HK_Backend.entity.ApplicationStatus;
import com.example.hk.HK_Backend.entity.GuardianRelation;
import com.example.hk.HK_Backend.entity.IdProofType;
import com.example.hk.HK_Backend.entity.PaymentMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class ApplicationResponse {
    private Long id;
    private String fullName;
    private String mobile;
    private String alternateMobile;
    private String email;
    private String address;
    private String city;
    private String state;
    private String occupation;
    private String institutionName;
    private String courseOrRole;
    private String guardianName;
    private String guardianContact;
    private GuardianRelation guardianRelation;
    private LocalDate joiningDate;
    private Integer durationMonths;
    private String roomTypeSlug;
    private String roomTypeTitle;
    private String roomNumber;
    private Integer bedNumber;  // the specific bed number assigned (from bedStart)
    private BigDecimal depositAmount;
    private PaymentMode paymentMode;
    private String transactionId;
    private IdProofType idProofType;
    private String idProofUrl;
    private String profilePhotoUrl;
    private ApplicationStatus status;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Payment statuses
    private com.example.hk.HK_Backend.entity.PaymentStatus depositStatus;
    private com.example.hk.HK_Backend.entity.PaymentStatus rentStatus;
}
