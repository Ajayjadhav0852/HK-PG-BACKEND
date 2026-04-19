package com.example.hk.HK_Backend.dto;

import com.example.hk.HK_Backend.entity.ApplicationStatus;
import com.example.hk.HK_Backend.entity.PaymentStatus;
import lombok.Data;

@Data
public class ApplicationStatusUpdateRequest {
    private ApplicationStatus status;      // nullable — only update if provided
    private String adminNotes;
    private PaymentStatus depositStatus;   // nullable — only update if provided
    private PaymentStatus rentStatus;      // nullable — only update if provided
}
