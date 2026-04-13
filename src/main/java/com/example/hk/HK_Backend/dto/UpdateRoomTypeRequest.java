package com.example.hk.HK_Backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Admin-only request to update a room type's price and/or image URL.
 * All fields are optional — only non-null fields are updated.
 */
@Data
public class UpdateRoomTypeRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly price must be greater than 0")
    private BigDecimal monthlyPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Security deposit must be greater than 0")
    private BigDecimal securityDeposit;

    /** Google Drive share link or any direct image URL */
    @Size(max = 1000, message = "Image URL must be under 1000 characters")
    private String imageUrl;

    @Size(max = 500, message = "Description must be under 500 characters")
    private String description;
}
