package com.example.hk.HK_Backend.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DashboardStatsDto {
    /** Active students = PENDING + CONFIRMED applications (not deleted/rejected) */
    private long totalStudents;
    /** Total registered user accounts with STUDENT role */
    private long registeredStudents;
    private long pendingApplications;
    private long confirmedApplications;
    private long rejectedApplications;
    private int  totalBeds;
    private int  occupiedBeds;
    private int  vacantBeds;
}
