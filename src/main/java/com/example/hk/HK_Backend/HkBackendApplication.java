package com.example.hk.HK_Backend;

import com.example.hk.HK_Backend.entity.Role;
import com.example.hk.HK_Backend.entity.RoomType;
import com.example.hk.HK_Backend.entity.User;
import com.example.hk.HK_Backend.repository.RoomTypeRepository;
import com.example.hk.HK_Backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@SpringBootApplication
public class HkBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HkBackendApplication.class, args);
    }

    /**
     * Seeds initial data on first startup.
     * Idempotent — skips if data already exists.
     */
    @Bean
    CommandLineRunner seedData(UserRepository userRepo,
                               RoomTypeRepository roomTypeRepo,
                               PasswordEncoder encoder) {
        return args -> {
            // ── Seed admin user ──────────────────────────────────────────────
            if (userRepo.findByEmail("admin@hkpg.com").isEmpty()) {
                User admin = new User();
                admin.setName("Admin User");
                admin.setEmail("admin@hkpg.com");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                userRepo.save(admin);
            }

            // ── Seed room types ──────────────────────────────────────────────
            if (roomTypeRepo.count() == 0) {
                // ── Replace these image URLs with your Cloudinary URLs ──────
                // Go to Admin Dashboard → Room Type Management → ✏️ Edit to update images
                // Or update these URLs here and reset the DB
                roomTypeRepo.save(buildRoomType("1-sharing", "1 Sharing - Private",
                        "Most Private", new BigDecimal("8500"), new BigDecimal("10000"),
                        "https://res.cloudinary.com/dzr0crkvr/image/upload/w_800,q_auto,f_auto/v1776062518/IMG_20260328_195806.jpg_exlqfb.jpg",
                        "Your own private space with attached bathroom, study desk, and wardrobe.", 1));

                roomTypeRepo.save(buildRoomType("2-sharing", "2 Sharing - Popular",
                        "Most Popular", new BigDecimal("6000"), new BigDecimal("8000"),
                        "https://res.cloudinary.com/dzr0crkvr/image/upload/w_800,q_auto,f_auto/v1776062505/WhatsApp_Image_2026-03-27_at_8.37.11_PM_qvrvk4.jpg",
                        "Share with one roommate. Spacious room with individual storage, AC, and great natural light.", 2));

                roomTypeRepo.save(buildRoomType("3-sharing", "3 Sharing",
                        "Affordable", new BigDecimal("5000"), new BigDecimal("6000"),
                        "https://res.cloudinary.com/dzr0crkvr/image/upload/w_800,q_auto,f_auto/v1776062505/IMG_20260413_102220113.jpg_bzsaya.jpg",
                        "Affordable triple-sharing with personal lockers, ceiling fans, and a friendly community vibe.", 3));

                roomTypeRepo.save(buildRoomType("4-sharing", "4 Sharing - Budget",
                        "Best Value", new BigDecimal("4000"), new BigDecimal("5000"),
                        "https://res.cloudinary.com/dzr0crkvr/image/upload/w_800,q_auto,f_auto/v1776062499/IMG_20260413_102118487.jpg_cqtzls.jpg",
                        "Most economical option. Great for students who want to save while enjoying all HKPG amenities.", 4));
            }
        };
    }

    private RoomType buildRoomType(String slug, String title, String tag,
                                   BigDecimal price, BigDecimal deposit,
                                   String image, String description, int bedsPerRoom) {
        RoomType rt = new RoomType();
        rt.setSlug(slug);
        rt.setTitle(title);
        rt.setTag(tag);
        rt.setMonthlyPrice(price);
        rt.setSecurityDeposit(deposit);
        rt.setImageUrl(image);
        rt.setDescription(description);
        rt.setBedsPerRoom(bedsPerRoom);
        return rt;
    }
}
