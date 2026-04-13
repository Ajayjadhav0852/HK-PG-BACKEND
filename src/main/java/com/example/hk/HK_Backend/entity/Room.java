package com.example.hk.HK_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical room in the HK PG building.
 *
 * Relationships:
 *   Room >── RoomType    (many rooms belong to one type)
 *   Room ──< Application (one room can have many applications over time)
 *
 * Key business logic:
 *   - occupiedBeds is incremented when a student submits an application
 *   - occupiedBeds is decremented when an application is rejected
 *   - vacantBeds = roomType.bedsPerRoom - occupiedBeds
 *   - isFull()   = occupiedBeds >= roomType.bedsPerRoom
 */
@Entity
@Table(
    name = "rooms",
    indexes = {
        @Index(name = "idx_rooms_room_number", columnList = "room_number", unique = true),
        @Index(name = "idx_rooms_room_type",   columnList = "room_type_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable room number: "Room 1", "Room 2", ... "Room 9" */
    @Column(name = "room_number", nullable = false, unique = true, length = 20)
    private String roomNumber;

    /** Floor label: "1st Floor" or "2nd Floor" */
    @Column(length = 50)
    private String floor;

    /** Current number of occupied beds — updated on every application submit/reject */
    @Column(name = "occupied_beds", nullable = false)
    @Builder.Default
    private int occupiedBeds = 0;

    /**
     * The starting bed number for this room (for display).
     * Room 1 starts at 1, Room 2 at 4, Room 3 at 8, etc.
     */
    @Column(name = "bed_start", nullable = false)
    @Builder.Default
    private int bedStart = 1;

    // Many rooms → one RoomType
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_room_room_type"))
    private RoomType roomType;

    // One room → many applications (history of who lived/applied here)
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Application> applications = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Computed helpers ──────────────────────────────────────────────────────

    @Transient
    public int getVacantBeds() {
        return roomType.getBedsPerRoom() - occupiedBeds;
    }

    @Transient
    public boolean isFull() {
        return occupiedBeds >= roomType.getBedsPerRoom();
    }
}
