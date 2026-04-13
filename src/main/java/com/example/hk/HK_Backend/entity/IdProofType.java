package com.example.hk.HK_Backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum IdProofType {
    AADHAAR_CARD, PAN_CARD, COLLEGE_ID, PASSPORT;

    @JsonCreator
    public static IdProofType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return IdProofType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
