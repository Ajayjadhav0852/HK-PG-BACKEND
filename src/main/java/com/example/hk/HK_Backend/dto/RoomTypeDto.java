package com.example.hk.HK_Backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class RoomTypeDto {
    private Long id;
    private String slug;
    private String title;
    private String tag;
    private BigDecimal monthlyPrice;
    private BigDecimal securityDeposit;
    private String imageUrl;
    private String description;
    private int bedsPerRoom;
    private int totalRooms;
    private int totalBeds;
    private int occupiedBeds;
    private int vacantBeds;
    private int vacantRooms;
    private List<RoomDto> rooms;
}
