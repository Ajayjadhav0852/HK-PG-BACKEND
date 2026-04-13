package com.example.hk.HK_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category of room (e.g. "1-sharing", "3-sharing").
 *
 * Relationships:
 *   RoomType ──< Room        (one type has many physical rooms)
 *   RoomType ──< Application (one type referenced by many applications)
 *
 * Note: Cascade is PERSIST+MERGE only — we never want deleting a RoomType
 *       to cascade-delete all rooms and applications.
 */
@Entity
@Table(
    name = "room_types",
    indexes = {
        @Index(name = "idx_room_types_slug", columnList = "slug", unique = true)
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** URL-friendly identifier: "1-sharing", "2-sharing", "3-sharing", "4-sharing" */
    @Column(nullable = false, unique = true, length = 30)
    private String slug;

    @Column(nullable = false, length = 100)
    private String title;

    /** Short label shown on cards: "Most Private", "Most Popular", etc. */
    @Column(length = 50)
    private String tag;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "security_deposit", nullable = false, precision = 10, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** How many beds are in each room of this type */
    @Column(name = "beds_per_room", nullable = false)
    private int bedsPerRoom;

    // One RoomType → many Rooms
    // NO orphanRemoval — we don't want to accidentally delete rooms
    @OneToMany(mappedBy = "roomType", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();
}
