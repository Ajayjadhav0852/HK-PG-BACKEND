package com.example.hk.HK_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a student's PG admission application.
 *
 * Relationships:
 *   Application >── User     (optional — null for walk-in applicants)
 *   Application >── Room     (assigned room — set on submit, may change on confirm)
 *   Application >── RoomType (the type of room requested)
 *
 * Note: room_type_id is kept separately from room_id so we can query
 *       "all applications for 3-sharing" even before a specific room is assigned.
 *
 * Status lifecycle:
 *   PENDING → CONFIRMED : bed already occupied (incremented on submit)
 *   PENDING → REJECTED  : bed freed (decremented)
 *   CONFIRMED → REJECTED: bed freed (decremented)
 *   REJECTED → PENDING  : bed re-occupied (incremented)
 */
@Entity
@Table(
    name = "applications",
    indexes = {
        @Index(name = "idx_app_user",        columnList = "user_id"),
        @Index(name = "idx_app_room",        columnList = "room_id"),
        @Index(name = "idx_app_room_type",   columnList = "room_type_id"),
        @Index(name = "idx_app_status",      columnList = "status"),
        @Index(name = "idx_app_created_at",  columnList = "created_at")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Applicant ─────────────────────────────────────────────────────────────
    /** Null for walk-in applicants who haven't registered */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",
                foreignKey = @ForeignKey(name = "fk_application_user"))
    private User user;

    // ── Personal Info ─────────────────────────────────────────────────────────
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    // ── Contact ───────────────────────────────────────────────────────────────
    @Column(name = "mobile", nullable = false, length = 15)
    private String mobile;

    @Column(name = "alternate_mobile", length = 15)
    private String alternateMobile;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    // ── Address ───────────────────────────────────────────────────────────────
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    // ── Academic / Work ───────────────────────────────────────────────────────
    @Column(length = 50)
    private String occupation;

    @Column(name = "institution_name", length = 200)
    private String institutionName;

    @Column(name = "course_or_role", length = 200)
    private String courseOrRole;

    // ── Guardian ──────────────────────────────────────────────────────────────
    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Column(name = "guardian_contact", length = 15)
    private String guardianContact;

    @Enumerated(EnumType.STRING)
    @Column(name = "guardian_relation", length = 20)
    private GuardianRelation guardianRelation;

    // ── Stay ──────────────────────────────────────────────────────────────────
    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "duration_months")
    private Integer durationMonths;

    // ── Room Assignment ───────────────────────────────────────────────────────
    /**
     * The specific physical room assigned to this applicant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",
                foreignKey = @ForeignKey(name = "fk_application_room"))
    private Room room;

    /**
     * The room type requested (1-sharing, 2-sharing, etc.).
     * Kept separately so we can query by type even if room changes.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_application_room_type"))
    private RoomType roomType;

    /** The specific bed number the student selected (e.g. 12 for R5) */
    @Column(name = "bed_number")
    private Integer bedNumber;

    // ── Payment ───────────────────────────────────────────────────────────────
    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 30)
    private PaymentMode paymentMode;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    // ── ID Proof ──────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "id_proof_type", length = 30)
    private IdProofType idProofType;

    @Column(name = "id_proof_url", length = 500)
    private String idProofUrl;

    // ── Status ────────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
