package com.example.hk.HK_Backend.controller;

import com.example.hk.HK_Backend.dto.ApiResponse;
import com.example.hk.HK_Backend.dto.RoomDto;
import com.example.hk.HK_Backend.dto.RoomTypeDto;
import com.example.hk.HK_Backend.entity.ApplicationStatus;
import com.example.hk.HK_Backend.repository.ApplicationRepository;
import com.example.hk.HK_Backend.repository.RoomRepository;
import com.example.hk.HK_Backend.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Rooms", description = "Public endpoints for room types and vacancy. No auth required.")
@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
@SecurityRequirements
public class RoomController {

    private final RoomService roomService;
    private final ApplicationRepository applicationRepository;
    private final RoomRepository roomRepository;

    @Operation(summary = "Get all room types with live vacancy stats")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomTypeDto>>> getAllRoomTypes() {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getAllRoomTypes()));
    }

    @Operation(summary = "Get all individual rooms — used by booking form for room selection",
               description = "Returns all 9 rooms with bed numbers, floor, type, and vacancy status.")
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAllRooms() {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getAllRooms()));
    }

    @Operation(summary = "Get a single room type by slug")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room type detail"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<RoomTypeDto>> getRoomType(
            @Parameter(description = "Room type slug", example = "3-sharing")
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getRoomTypeBySlug(slug)));
    }

    @Operation(
        summary = "Get booked bed numbers for a room (PENDING + CONFIRMED)",
        description = "Returns list of bed numbers that are already booked (PENDING or CONFIRMED). " +
                      "Frontend uses this to disable those beds in the booking form dropdown."
    )
    @GetMapping("/booked-beds/{roomNumber}")
    public ResponseEntity<ApiResponse<List<Integer>>> getBookedBeds(
            @Parameter(description = "Room number e.g. R2", example = "R2")
            @PathVariable String roomNumber) {
        List<Integer> bookedBeds = roomRepository.findByRoomNumber(roomNumber)
                .map(room -> applicationRepository.findBookedBedNumbers(room))
                .orElse(List.of());
        return ResponseEntity.ok(ApiResponse.ok(bookedBeds));
    }
}
