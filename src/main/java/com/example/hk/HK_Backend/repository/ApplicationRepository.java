package com.example.hk.HK_Backend.repository;

import com.example.hk.HK_Backend.entity.Application;
import com.example.hk.HK_Backend.entity.ApplicationStatus;
import com.example.hk.HK_Backend.entity.Room;
import com.example.hk.HK_Backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /** All applications for a specific user, newest first */
    List<Application> findByUserOrderByCreatedAtDesc(User user);

    /** All applications with a given status */
    List<Application> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);

    /** All applications, newest first */
    List<Application> findAllByOrderByCreatedAtDesc();

    /** Count by status — used for dashboard stats */
    long countByStatus(ApplicationStatus status);

    /** Count active (non-rejected) applications for a room — prevents overbooking */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.room = :room AND a.status != 'REJECTED'")
    long countActiveByRoom(@Param("room") Room room);

    /** Check if a user already has a pending/confirmed application for a room type */
    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.user = :user AND a.roomType.slug = :slug AND a.status IN ('PENDING', 'CONFIRMED')")
    boolean hasActiveApplicationForType(@Param("user") User user, @Param("slug") String slug);
}
