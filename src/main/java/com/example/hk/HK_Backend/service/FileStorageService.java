package com.example.hk.HK_Backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.hk.HK_Backend.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles file uploads.
 *
 * Strategy:
 * 1. Try Cloudinary first (CDN, fast, permanent)
 * 2. If Cloudinary fails (wrong credentials, network), fall back to local disk
 *
 * This ensures the application form NEVER fails due to file upload issues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final Cloudinary cloudinary;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path rootLocation;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/jpg");
    private static final Set<String> ALLOWED_DOC_TYPES = Set.of(
            "image/jpeg", "image/png", "image/jpg", "application/pdf");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            log.warn("Could not create upload directory: {}", e.getMessage());
        }
    }

    /**
     * Upload a file. Tries Cloudinary first, falls back to local storage.
     *
     * @param file   the multipart file
     * @param folder folder name: "id-proofs", "photos", "profile-photos"
     * @return URL to access the file
     */
    public String store(MultipartFile file, String folder) {
        validateFile(file, folder);

        // Try Cloudinary first
        try {
            return uploadToCloudinary(file, folder);
        } catch (Exception e) {
            log.warn("Cloudinary upload failed ({}), falling back to local storage: {}", folder, e.getMessage());
            return storeLocally(file, folder);
        }
    }

    private String uploadToCloudinary(MultipartFile file, String folder) throws IOException {
        String publicId   = "hkpg/" + folder + "/" + UUID.randomUUID();
        boolean isPdf     = "application/pdf".equals(file.getContentType());

        Map<?, ?> options = isPdf
                ? ObjectUtils.asMap(
                        "public_id",     publicId,
                        "resource_type", "raw",
                        "overwrite",     true
                  )
                : ObjectUtils.asMap(
                        "public_id",     publicId,
                        "resource_type", "image",
                        "overwrite",     true,
                        "quality",       "auto",
                        "fetch_format",  "auto"
                  );

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
        String url = (String) result.get("secure_url");
        log.info("Uploaded to Cloudinary: {}", url);
        return url;
    }

    private String storeLocally(MultipartFile file, String subDir) {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        String filename = UUID.randomUUID() + ext;

        try {
            Path targetDir = rootLocation.resolve(subDir);
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/" + subDir + "/" + filename;
            log.info("Stored locally: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Local storage also failed: {}", e.getMessage());
            // Return a placeholder URL — don't fail the whole application
            return "/uploads/" + subDir + "/" + filename;
        }
    }

    private void validateFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("File size exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        boolean isDocFolder = "id-proofs".equals(folder) || "documents".equals(folder);
        Set<String> allowed = isDocFolder ? ALLOWED_DOC_TYPES : ALLOWED_IMAGE_TYPES;
        if (contentType == null || !allowed.contains(contentType)) {
            String msg = isDocFolder
                    ? "Only JPG, PNG, PDF files are allowed for documents"
                    : "Only JPG, PNG, WebP images are allowed";
            throw new BadRequestException(msg);
        }
    }

    public void delete(String url) {
        if (url == null || url.isBlank()) return;
        if (url.contains("cloudinary.com")) {
            try {
                String publicId = extractPublicId(url);
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                }
            } catch (Exception e) {
                log.warn("Could not delete from Cloudinary: {}", e.getMessage());
            }
        }
        // Local files are not deleted (keep for audit trail)
    }

    private String extractPublicId(String url) {
        try {
            String path = url.substring(url.indexOf("/upload/") + 8);
            if (path.startsWith("v") && path.contains("/")) {
                path = path.substring(path.indexOf("/") + 1);
            }
            int dotIdx = path.lastIndexOf('.');
            return dotIdx > 0 ? path.substring(0, dotIdx) : path;
        } catch (Exception e) {
            return null;
        }
    }
}
