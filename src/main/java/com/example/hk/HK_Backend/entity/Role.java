package com.example.hk.HK_Backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    ADMIN, STUDENT;

    @JsonCreator
    public static Role fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
