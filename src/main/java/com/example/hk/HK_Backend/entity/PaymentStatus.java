package com.example.hk.HK_Backend.entity;

public enum PaymentStatus {
    PENDING,    // Not yet paid
    RECEIVED,   // Payment confirmed by admin
    OVERDUE     // Payment overdue
}
