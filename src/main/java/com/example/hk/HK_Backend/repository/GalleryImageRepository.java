package com.example.hk.HK_Backend.repository;

import com.example.hk.HK_Backend.entity.GalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {
    List<GalleryImage> findBySectionOrderByDisplayOrderAscCreatedAtDesc(String section);
    List<GalleryImage> findAllByOrderBySectionAscDisplayOrderAscCreatedAtDesc();
}
