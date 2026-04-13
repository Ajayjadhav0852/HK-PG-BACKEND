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
 * ═══════════════════════════════════════════════════════════════════
 *  HK PG — FINAL CONFIRMED ROOM LAYOUT
 * ═══════════════════════════════════════════════════════════════════
 *
 *  FLOOR 1  (Rooms 1–4)
 *  ┌────────┬──────────────┬──────────────────────┬──────┐
 *  │ Room   │ Type         │ Bed Numbers          │ Beds │
 *  ├────────┼──────────────┼──────────────────────┼──────┤
 *  │ Room 1 │ 3-sharing    │ 1, 2, 3              │  3   │
 *  │ Room 2 │ 4-sharing    │ 4, 5, 6, 7           │  4   │
 *  │ Room 3 │ 1-sharing    │ 8                    │  1   │
 *  │ Room 4 │ 3-sharing    │ 9, 10, 11            │  3   │
 *  └────────┴──────────────┴──────────────────────┴──────┘
 *  Floor 1 total: 11 beds
 *
 *  FLOOR 2  (Rooms 5–9)
 *  ┌────────┬──────────────┬──────────────────────┬──────┐
 *  │ Room   │ Type         │ Bed Numbers          │ Beds │
 *  ├────────┼──────────────┼──────────────────────┼──────┤
 *  │ Room 5 │ 2-sharing    │ 12, 13               │  2   │
 *  │ Room 6 │ 3-sharing    │ 14, 15, 16           │  3   │
 *  │ Room 7 │ 4-sharing    │ 17, 18, 19, 20       │  4   │
 *  │ Room 8 │ 3-sharing    │ 21, 22, 23           │  3   │
 *  │ Room 9 │ 3-sharing    │ 24, 25, 26           │  3   │
 *  └────────┴──────────────┴──────────────────────┴──────┘
 *  Floor 2 total: 15 beds
 *
 *  GRAND TOTAL: 26 beds across 9 rooms
 *
 *  BY ROOM TYPE:
 *  ┌──────────────┬──────────────────┬──────────────────────────────────┐
 *  │ Type         │ Rooms            │ Bed Numbers                      │
 *  ├──────────────┼──────────────────┼──────────────────────────────────┤
 *  │ 1-sharing    │ R3               │ 8                                │
 *  │ 2-sharing    │ R5               │ 12, 13                           │
 *  │ 3-sharing    │ R1,R4,R6,R8,R9   │ 1-3, 9-11, 14-16, 21-23, 24-26  │
 *  │ 4-sharing    │ R2, R7           │ 4-7, 17-20                       │
 *  └──────────────┴──────────────────┴──────────────────────────────────┘
 * ═══════════════════════════════════════════════════════════════════
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
        if (roomRepository.count() > 0) {
            log.info("Rooms already seeded — skipping.");
            return;
        }

        // [roomNumber, floor, typeSlug, bedStart]
        Object[][] rooms = {
            // ── Floor 1 ──────────────────────────────────────────────────────
            { "R1", "1st Floor", "3-sharing",  1 },   // Beds 1,2,3
            { "R2", "1st Floor", "4-sharing",  4 },   // Beds 4,5,6,7
            { "R3", "1st Floor", "1-sharing",  8 },   // Bed  8
            { "R4", "1st Floor", "3-sharing",  9 },   // Beds 9,10,11
            // ── Floor 2 ──────────────────────────────────────────────────────
            { "R5", "2nd Floor", "2-sharing", 12 },   // Beds 12,13
            { "R6", "2nd Floor", "3-sharing", 14 },   // Beds 14,15,16
            { "R7", "2nd Floor", "4-sharing", 17 },   // Beds 17,18,19,20
            { "R8", "2nd Floor", "3-sharing", 21 },   // Beds 21,22,23
            { "R9", "2nd Floor", "3-sharing", 24 },   // Beds 24,25,26
        };

        int totalRooms = 0;
        int totalBeds  = 0;

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

            int bedEnd = bedStart + rt.getBedsPerRoom() - 1;
            log.info("  {} | {} | {} | Beds {}{} | {} beds",
                    roomNumber, floor, rt.getTitle(),
                    bedStart,
                    rt.getBedsPerRoom() > 1 ? "–" + bedEnd : "",
                    rt.getBedsPerRoom());

            totalRooms++;
            totalBeds += rt.getBedsPerRoom();
        }

        log.info("══════════════════════════════════════════════════");
        log.info("HK PG seeding complete: {} rooms, {} total beds", totalRooms, totalBeds);
        log.info("  1-sharing: R3          → Bed 8");
        log.info("  2-sharing: R5          → Beds 12–13");
        log.info("  3-sharing: R1,R4,R6,R8,R9 → Beds 1–3,9–11,14–16,21–23,24–26");
        log.info("  4-sharing: R2,R7       → Beds 4–7,17–20");
        log.info("══════════════════════════════════════════════════");
    }
}
