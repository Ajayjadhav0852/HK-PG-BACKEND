package com.example.hk.HK_Backend.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMode {
    CASH, UPI, ONLINE_TRANSFER;

    @JsonCreator
    public static PaymentMode fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return PaymentMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
