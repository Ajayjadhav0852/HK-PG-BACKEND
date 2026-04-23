package com.example.hk.HK_Backend.service;

import com.example.hk.HK_Backend.dto.GalleryImageDto;
import com.example.hk.HK_Backend.entity.GalleryImage;
import com.example.hk.HK_Backend.exception.BadRequestException;
import com.example.hk.HK_Backend.repository.GalleryImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryService {

    private final GalleryImageRepository galleryImageRepository;
    private final FileStorageService fileStorageService;

    private static final Set<String> VALID_SECTIONS = Set.of(
            "outdoor", "indoor", "rooms", "balcony", "terrace", "bathrooms", "common",
            "1-sharing", "2-sharing", "3-sharing", "4-sharing", "parking"
    );

    public List<GalleryImageDto> getAllImages() {
        return galleryImageRepository.findAllByOrderBySectionAscDisplayOrderAscCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<GalleryImageDto> getImagesBySection(String section) {
        validateSection(section);
        return galleryImageRepository
                .findBySectionOrderByDisplayOrderAscCreatedAtDesc(section)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public GalleryImageDto uploadImage(MultipartFile file, String section, String caption, Integer displayOrder) {
        validateSection(section);

        String url = fileStorageService.store(file, "gallery/" + section);

        GalleryImage image = GalleryImage.builder()
                .imageUrl(url)
                .section(section)
                .caption(caption != null ? caption.trim() : "")
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .build();

        GalleryImage saved = galleryImageRepository.save(image);
        log.info("Gallery image uploaded: section={}, url={}", section, url);
        return toDto(saved);
    }

    /** Save a Cloudinary URL that was uploaded directly from the frontend */
    public GalleryImageDto saveImageUrl(String imageUrl, String section, String caption, Integer displayOrder) {
        validateSection(section);
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BadRequestException("imageUrl is required");
        }

        GalleryImage image = GalleryImage.builder()
                .imageUrl(imageUrl.trim())
                .section(section)
                .caption(caption != null ? caption.trim() : "")
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .build();

        GalleryImage saved = galleryImageRepository.save(image);
        log.info("Gallery image URL saved: section={}, url={}", section, imageUrl);
        return toDto(saved);
    }

    public void deleteImage(Long id) {
        GalleryImage image = galleryImageRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Gallery image not found with id: " + id));
        fileStorageService.delete(image.getImageUrl());
        galleryImageRepository.delete(image);
        log.info("Gallery image deleted: id={}", id);
    }

    private void validateSection(String section) {
        if (section == null || !VALID_SECTIONS.contains(section.toLowerCase())) {
            throw new BadRequestException("Invalid section. Must be one of: " + VALID_SECTIONS);
        }
    }

    private GalleryImageDto toDto(GalleryImage img) {
        return GalleryImageDto.builder()
                .id(img.getId())
                .imageUrl(img.getImageUrl())
                .section(img.getSection())
                .caption(img.getCaption())
                .displayOrder(img.getDisplayOrder())
                .createdAt(img.getCreatedAt())
                .build();
    }
}
