package com.example.hk.HK_Backend.service;

import com.example.hk.HK_Backend.dto.RoomDto;
import com.example.hk.HK_Backend.dto.RoomTypeDto;
import com.example.hk.HK_Backend.entity.Room;
import com.example.hk.HK_Backend.entity.RoomType;
import com.example.hk.HK_Backend.exception.BadRequestException;
import com.example.hk.HK_Backend.exception.ResourceNotFoundException;
import com.example.hk.HK_Backend.repository.RoomRepository;
import com.example.hk.HK_Backend.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public List<RoomTypeDto> getAllRoomTypes() {
        return roomTypeRepository.findAll().stream()
                .map(this::toRoomTypeDto)
                .collect(Collectors.toList());
    }

    /** Returns ALL rooms across all types — used by the application form dropdown */
    @Transactional(readOnly = true)
    public List<RoomDto> getAllRooms() {
        return roomRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(r -> {
                    // Sort by room number numerically: "Room 1" → 1
                    try { return Integer.parseInt(r.getRoomNumber().replaceAll("[^0-9]", "")); }
                    catch (Exception e) { return 0; }
                }))
                .map(this::toRoomDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomTypeDto getRoomTypeBySlug(String slug) {
        RoomType rt = roomTypeRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found: " + slug));
        return toRoomTypeDto(rt);
    }

    /**
     * Admin: update price and/or image URL of a room type.
     * Only non-null fields in the request are applied.
     */
    @Transactional
    public RoomTypeDto updateRoomType(String slug, com.example.hk.HK_Backend.dto.UpdateRoomTypeRequest req) {
        RoomType rt = roomTypeRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found: " + slug));

        if (req.getMonthlyPrice()    != null) rt.setMonthlyPrice(req.getMonthlyPrice());
        if (req.getSecurityDeposit() != null) rt.setSecurityDeposit(req.getSecurityDeposit());
        if (req.getImageUrl()        != null) rt.setImageUrl(convertGoogleDriveUrl(req.getImageUrl().trim()));
        if (req.getDescription()     != null) rt.setDescription(req.getDescription().trim());

        roomTypeRepository.save(rt);
        return toRoomTypeDto(rt);
    }

    /**
     * Converts a Google Drive share link to a direct image URL.
     *
     * Google Drive share link format:
     *   https://drive.google.com/file/d/FILE_ID/view?usp=sharing
     *
     * Direct image URL format:
     *   https://drive.google.com/uc?export=view&id=FILE_ID
     *
     * If the URL is not a Google Drive link, it is returned as-is.
     */
    public static String convertGoogleDriveUrl(String url) {
        if (url == null || url.isBlank()) return url;

        // Pattern: https://drive.google.com/file/d/FILE_ID/view...
        if (url.contains("drive.google.com/file/d/")) {
            try {
                String fileId = url.split("/file/d/")[1].split("/")[0];
                return "https://drive.google.com/uc?export=view&id=" + fileId;
            } catch (Exception e) {
                // Fall through — return original URL
            }
        }

        // Pattern: https://drive.google.com/open?id=FILE_ID
        if (url.contains("drive.google.com/open?id=")) {
            try {
                String fileId = url.split("id=")[1].split("&")[0];
                return "https://drive.google.com/uc?export=view&id=" + fileId;
            } catch (Exception e) {
                // Fall through
            }
        }

        return url; // Not a Google Drive link — use as-is
    }

    @Transactional
    public void incrementOccupancy(RoomType roomType) {
        List<Room> vacantRooms = roomRepository.findVacantRoomsByType(roomType);
        if (vacantRooms.isEmpty()) {
            throw new BadRequestException("No vacant beds available in " + roomType.getTitle());
        }
        Room room = vacantRooms.get(0);
        room.setOccupiedBeds(room.getOccupiedBeds() + 1);
        roomRepository.save(room);
    }

    @Transactional
    public void decrementOccupancy(Room room) {
        if (room.getOccupiedBeds() > 0) {
            room.setOccupiedBeds(room.getOccupiedBeds() - 1);
            roomRepository.save(room);
        }
    }

    // ── Seed helper: add rooms to a room type ─────────────────────────────────
    @Transactional
    public void seedRoomsForType(String slug, List<String[]> roomData) {
        RoomType rt = roomTypeRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found: " + slug));

        if (!rt.getRooms().isEmpty()) return; // already seeded

        for (String[] data : roomData) {
            Room room = new Room();
            room.setRoomNumber(data[0]);
            room.setFloor(data[1]);
            room.setOccupiedBeds(Integer.parseInt(data[2]));
            room.setRoomType(rt);
            roomRepository.save(room);
        }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────
    public RoomTypeDto toRoomTypeDto(RoomType rt) {
        List<Room> rooms = roomRepository.findByRoomTypeOrderByRoomNumberAsc(rt);
        int totalBeds = rooms.size() * rt.getBedsPerRoom();
        int occupiedBeds = rooms.stream().mapToInt(Room::getOccupiedBeds).sum();
        int vacantBeds = totalBeds - occupiedBeds;
        int vacantRooms = (int) rooms.stream().filter(r -> r.getOccupiedBeds() < rt.getBedsPerRoom()).count();

        return RoomTypeDto.builder()
                .id(rt.getId())
                .slug(rt.getSlug())
                .title(rt.getTitle())
                .tag(rt.getTag())
                .monthlyPrice(rt.getMonthlyPrice())
                .securityDeposit(rt.getSecurityDeposit())
                .imageUrl(rt.getImageUrl())
                .description(rt.getDescription())
                .bedsPerRoom(rt.getBedsPerRoom())
                .totalRooms(rooms.size())
                .totalBeds(totalBeds)
                .occupiedBeds(occupiedBeds)
                .vacantBeds(vacantBeds)
                .vacantRooms(vacantRooms)
                .rooms(rooms.stream().map(this::toRoomDto).collect(Collectors.toList()))
                .build();
    }

    private RoomDto toRoomDto(Room r) {
        return RoomDto.builder()
                .id(r.getId())
                .roomNumber(r.getRoomNumber())
                .floor(r.getFloor())
                .roomTypeSlug(r.getRoomType().getSlug())
                .roomTypeTitle(r.getRoomType().getTitle())
                .bedsPerRoom(r.getRoomType().getBedsPerRoom())
                .bedStart(r.getBedStart())
                .occupiedBeds(r.getOccupiedBeds())
                .vacantBeds(r.getVacantBeds())
                .full(r.isFull())
                .build();
    }

}
