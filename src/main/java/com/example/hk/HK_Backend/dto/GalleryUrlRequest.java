package com.example.hk.HK_Backend.dto;

import lombok.Data;

@Data
public class GalleryUrlRequest {
    private String imageUrl;
    private String section;
    private String caption;
    private Integer displayOrder;
}
