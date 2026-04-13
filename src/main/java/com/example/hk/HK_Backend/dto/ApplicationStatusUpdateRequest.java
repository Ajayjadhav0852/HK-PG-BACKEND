package com.example.hk.HK_Backend.dto;

import com.example.hk.HK_Backend.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationStatusUpdateRequest {
    @NotNull
    private ApplicationStatus status;
    private String adminNotes;
}
