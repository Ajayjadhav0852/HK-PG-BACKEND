package com.example.hk.HK_Backend.config;

import com.example.hk.HK_Backend.entity.Application;
import com.example.hk.HK_Backend.repository.ApplicationRepository;
import com.example.hk.HK_Backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Sends a Google Review reminder email to students 7 days after their joining date.
 * Runs daily at 10:00 AM IST.
 * Only sends once per student (googleReviewEmailSent flag prevents duplicates).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleReviewScheduler {

    private final ApplicationRepository applicationRepository;
    private final EmailService           emailService;

    // Runs every day at 10:00 AM (UTC+5:30 = 04:30 UTC)
    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void sendGoogleReviewReminders() {
        // Target: students whose joining date was exactly 7 days ago
        LocalDate targetDate = LocalDate.now().minusDays(7);
        List<Application> candidates = applicationRepository.findConfirmedForGoogleReview(targetDate);

        if (candidates.isEmpty()) {
            log.debug("[GoogleReview] No review reminders to send today (target date: {})", targetDate);
            return;
        }

        log.info("[GoogleReview] Sending review reminders to {} student(s) (joined: {})", candidates.size(), targetDate);

        for (Application app : candidates) {
            try {
                emailService.sendGoogleReviewReminder(
                    app.getEmail(),
                    app.getFullName(),
                    app.getRoomType() != null ? app.getRoomType().getTitle() : "your room"
                );
                app.setGoogleReviewEmailSent(true);
                applicationRepository.save(app);
                log.info("[GoogleReview] Sent to {} ({})", app.getFullName(), app.getEmail());
            } catch (Exception e) {
                log.warn("[GoogleReview] Failed for {} ({}): {}", app.getFullName(), app.getEmail(), e.getMessage());
            }
        }
    }
}
