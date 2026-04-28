package com.example.hk.HK_Backend.repository;

import com.example.hk.HK_Backend.entity.Application;
import com.example.hk.HK_Backend.entity.ApplicationStatus;
import com.example.hk.HK_Backend.entity.Room;
import com.example.hk.HK_Backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /** All applications for a specific user, newest first — excludes soft-deleted */
    List<Application> findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(User user);

    /** All applications with a given status — excludes soft-deleted */
    List<Application> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(ApplicationStatus status);

    /** All active (non-deleted) applications, newest first */
    List<Application> findByDeletedAtIsNullOrderByCreatedAtDesc();

    /** ALL applications including deleted — for admin history view */
    List<Application> findAllByOrderByCreatedAtDesc();

    /** Count by status — used for dashboard stats (active only) */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.status = :status AND a.deletedAt IS NULL")
    long countByStatus(@Param("status") ApplicationStatus status);

    /** Count active (non-rejected) applications for a room — prevents overbooking */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.room = :room AND a.status != 'REJECTED' AND a.deletedAt IS NULL")
    long countActiveByRoom(@Param("room") Room room);

    /** Check if a user already has a pending/confirmed application for a room type */
    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.user = :user AND a.roomType.slug = :slug AND a.status IN ('PENDING', 'CONFIRMED') AND a.deletedAt IS NULL")
    boolean hasActiveApplicationForType(@Param("user") User user, @Param("slug") String slug);

    /** Check if a specific bed in a room is already booked (PENDING or CONFIRMED) */
    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.room = :room AND a.bedNumber = :bedNumber AND a.status != :status AND a.deletedAt IS NULL")
    boolean existsByRoomAndBedNumberAndStatusNot(@Param("room") Room room, @Param("bedNumber") Integer bedNumber, @Param("status") ApplicationStatus status);

    /** Get all booked bed numbers for a room (PENDING + CONFIRMED) */
    @Query("SELECT a.bedNumber FROM Application a WHERE a.room = :room AND a.status != 'REJECTED' AND a.bedNumber IS NOT NULL AND a.deletedAt IS NULL")
    List<Integer> findBookedBedNumbers(@Param("room") Room room);

    /** Find CONFIRMED applications where joining date was exactly 7 days ago and review email not yet sent */
    @Query("SELECT a FROM Application a WHERE a.status = 'CONFIRMED' AND a.joiningDate = :targetDate AND (a.googleReviewEmailSent IS NULL OR a.googleReviewEmailSent = false) AND a.deletedAt IS NULL AND a.email IS NOT NULL AND a.email != ''")
    List<Application> findConfirmedForGoogleReview(@Param("targetDate") LocalDate targetDate);

    /** Total students who ever lived here (CONFIRMED, including vacated/deleted) — for owner stats */
    @Query("SELECT COUNT(DISTINCT a.email) FROM Application a WHERE a.status = 'CONFIRMED'")
    long countTotalStudentsEver();

    /** Currently active students (CONFIRMED, not vacated, not deleted) */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.status = 'CONFIRMED' AND a.vacatedAt IS NULL AND a.deletedAt IS NULL")
    long countCurrentlyActive();
}
