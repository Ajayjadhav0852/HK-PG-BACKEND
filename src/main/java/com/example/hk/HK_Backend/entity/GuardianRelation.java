package com.example.hk.HK_Backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GuardianRelation {
    FATHER, MOTHER, OTHER;

    @JsonCreator
    public static GuardianRelation fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return GuardianRelation.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
