package com.example.hk.HK_Backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GalleryImageDto {
    private Long id;
    private String imageUrl;
    private String section;
    private String caption;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
