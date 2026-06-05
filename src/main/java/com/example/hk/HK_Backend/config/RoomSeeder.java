package com.example.hk.HK_Backend.config;

import com.example.hk.HK_Backend.entity.Room;
import com.example.hk.HK_Backend.entity.RoomType;
import com.example.hk.HK_Backend.repository.RoomRepository;
import com.example.hk.HK_Backend.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds rooms on first startup if none exist.
 * Idempotent — skips if rooms already present.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RoomSeeder implements ApplicationRunner {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository     roomRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            if (roomRepository.count() > 0) {
                log.info("Rooms already seeded — skipping.");
                return;
            }

            // [roomNumber, floor, typeSlug, bedStart]
            Object[][] rooms = {
                { "R1", "1st Floor", "3-sharing",  1 },
                { "R2", "1st Floor", "4-sharing",  4 },
                { "R3", "1st Floor", "1-sharing",  8 },
                { "R4", "1st Floor", "3-sharing",  9 },
                { "R5", "2nd Floor", "2-sharing", 12 },
                { "R6", "2nd Floor", "3-sharing", 14 },
                { "R7", "2nd Floor", "4-sharing", 17 },
                { "R8", "2nd Floor", "3-sharing", 21 },
                { "R9", "2nd Floor", "3-sharing", 24 },
            };

            for (Object[] data : rooms) {
                String roomNumber = (String)  data[0];
                String floor      = (String)  data[1];
                String slug       = (String)  data[2];
                int    bedStart   = (Integer) data[3];

                RoomType rt = roomTypeRepository.findBySlug(slug).orElse(null);
                if (rt == null) {
                    log.warn("RoomType '{}' not found — skipping {}", slug, roomNumber);
                    continue;
                }

                Room room = new Room();
                room.setRoomNumber(roomNumber);
                room.setFloor(floor);
                room.setBedStart(bedStart);
                room.setOccupiedBeds(0);
                room.setRoomType(rt);
                roomRepository.save(room);
                log.info("Seeded room: {} ({}, {})", roomNumber, floor, rt.getTitle());
            }
            log.info("Room seeding complete.");
        } catch (Exception e) {
            // Never crash the app due to seeding failure
            log.warn("Room seeding skipped (non-fatal): {}", e.getMessage());
        }
    }
}
