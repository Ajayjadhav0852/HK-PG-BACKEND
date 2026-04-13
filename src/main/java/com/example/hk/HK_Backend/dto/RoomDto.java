package com.example.hk.HK_Backend.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class RoomDto {
    private Long   id;
    private String roomNumber;   // "Room 1", "Room 2", ...
    private String floor;        // "1st Floor", "2nd Floor"
    private String roomTypeSlug; // "3-sharing", "4-sharing", etc.
    private String roomTypeTitle;
    private int    bedsPerRoom;
    private int    bedStart;     // first bed number in this room (e.g. Room 1 → 1, Room 2 → 4)
    private int    occupiedBeds;
    private int    vacantBeds;
    private boolean full;
}
