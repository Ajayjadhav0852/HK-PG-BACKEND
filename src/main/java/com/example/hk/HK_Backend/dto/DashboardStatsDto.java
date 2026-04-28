package com.example.hk.HK_Backend.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DashboardStatsDto {
    /** Currently active confirmed students (not vacated, not deleted) */
    private long totalStudents;
    /** Total students who ever lived here (all-time, including vacated) */
    private long totalStudentsEver;
    /** Total registered user accounts with STUDENT role */
    private long registeredStudents;
    private long pendingApplications;
    private long confirmedApplications;
    private long rejectedApplications;
    private int  totalBeds;
    private int  occupiedBeds;
    private int  vacantBeds;
}
