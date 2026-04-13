package com.example.hk.HK_Backend.service;

import com.example.hk.HK_Backend.dto.DashboardStatsDto;
import com.example.hk.HK_Backend.entity.ApplicationStatus;
import com.example.hk.HK_Backend.repository.ApplicationRepository;
import com.example.hk.HK_Backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ApplicationRepository applicationRepository;
    private final RoomRepository        roomRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {

        long pending   = applicationRepository.countByStatus(ApplicationStatus.PENDING);
        long confirmed = applicationRepository.countByStatus(ApplicationStatus.CONFIRMED);
        long rejected  = applicationRepository.countByStatus(ApplicationStatus.REJECTED);

        // Active Students = ONLY CONFIRMED applications
        // (students whose booking is confirmed by admin)
        // When all rejected/deleted → 0
        long activeStudents = confirmed;

        int totalBeds    = roomRepository.sumAllTotalBeds();
        int occupiedBeds = roomRepository.sumAllOccupiedBeds();

        return DashboardStatsDto.builder()
                .totalStudents(activeStudents)
                .pendingApplications(pending)
                .confirmedApplications(confirmed)
                .rejectedApplications(rejected)
                .totalBeds(totalBeds)
                .occupiedBeds(occupiedBeds)
                .vacantBeds(totalBeds - occupiedBeds)
                .build();
    }
}
