package com.example.hk.HK_Backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Self-ping scheduler — keeps the Render free-tier server awake.
 *
 * Render free tier sleeps after 15 min of inactivity.
 * This pings /health every 10 minutes so the JVM never goes cold.
 * Runs entirely server-side — no browser tab needed.
 */
@Slf4j
@Component
public class KeepAliveScheduler {

    @Value("${app.site.backend-url:https://hk-pg-backend.onrender.com}")
    private String backendUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Every 10 minutes — well within Render's 15-min sleep threshold
    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 60 * 1000)
    public void selfPing() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            log.debug("⏰ Keep-alive ping → {} ({})", backendUrl, res.statusCode());
        } catch (Exception e) {
            // Silent — don't spam logs on transient failures
            log.debug("⏰ Keep-alive ping failed: {}", e.getMessage());
        }
    }
}
