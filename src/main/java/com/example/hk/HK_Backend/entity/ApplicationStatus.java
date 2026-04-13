package com.example.hk.HK_Backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ApplicationStatus {
    PENDING, CONFIRMED, REJECTED;

    @JsonCreator
    public static ApplicationStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return ApplicationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
