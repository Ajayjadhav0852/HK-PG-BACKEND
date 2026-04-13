package com.example.hk.HK_Backend.repository;

import com.example.hk.HK_Backend.entity.Room;
import com.example.hk.HK_Backend.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByRoomTypeOrderByRoomNumberAsc(RoomType roomType);

    Optional<Room> findByRoomNumber(String roomNumber);

    /** Rooms that still have at least one vacant bed */
    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType AND r.occupiedBeds < r.roomType.bedsPerRoom ORDER BY r.roomNumber ASC")
    List<Room> findVacantRoomsByType(@Param("roomType") RoomType roomType);

    /** Total occupied beds across all rooms of a given type */
    @Query("SELECT COALESCE(SUM(r.occupiedBeds), 0) FROM Room r WHERE r.roomType = :roomType")
    int sumOccupiedBedsByType(@Param("roomType") RoomType roomType);

    /** Total occupied beds across ALL rooms */
    @Query("SELECT COALESCE(SUM(r.occupiedBeds), 0) FROM Room r")
    int sumAllOccupiedBeds();

    /** Total beds across ALL rooms */
    @Query("SELECT COALESCE(SUM(r.roomType.bedsPerRoom), 0) FROM Room r")
    int sumAllTotalBeds();

    /** Directly increment occupiedBeds — avoids stale read issues */
    @Modifying
    @Query("UPDATE Room r SET r.occupiedBeds = r.occupiedBeds + 1 WHERE r.id = :id AND r.occupiedBeds < r.roomType.bedsPerRoom")
    int incrementOccupiedBeds(@Param("id") Long id);

    /** Directly decrement occupiedBeds — avoids going below 0 */
    @Modifying
    @Query("UPDATE Room r SET r.occupiedBeds = r.occupiedBeds - 1 WHERE r.id = :id AND r.occupiedBeds > 0")
    int decrementOccupiedBeds(@Param("id") Long id);
}
