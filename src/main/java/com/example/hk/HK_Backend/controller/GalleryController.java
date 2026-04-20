package com.example.hk.HK_Backend.controller;

import com.example.hk.HK_Backend.dto.ApiResponse;
import com.example.hk.HK_Backend.dto.GalleryImageDto;
import com.example.hk.HK_Backend.service.GalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Gallery", description = "Gallery image management. GET endpoints are public. Upload/Delete require ADMIN role.")
@RestController
@RequestMapping("/api/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;

    @Operation(summary = "Get all gallery images grouped by section (public)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<GalleryImageDto>>> getAllImages() {
        return ResponseEntity.ok(ApiResponse.ok(galleryService.getAllImages()));
    }

    @Operation(summary = "Get gallery images by section (public)")
    @GetMapping("/section/{section}")
    public ResponseEntity<ApiResponse<List<GalleryImageDto>>> getBySection(
            @PathVariable String section) {
        return ResponseEntity.ok(ApiResponse.ok(galleryService.getImagesBySection(section)));
    }

    @Operation(summary = "Upload a gallery image (Admin only)", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GalleryImageDto>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("section") String section,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder) {
        GalleryImageDto dto = galleryService.uploadImage(file, section, caption, displayOrder);
        return ResponseEntity.ok(ApiResponse.ok("Image uploaded successfully", dto));
    }

    @Operation(summary = "Delete a gallery image (Admin only)", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long id) {
        galleryService.deleteImage(id);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted successfully", null));
    }
}
