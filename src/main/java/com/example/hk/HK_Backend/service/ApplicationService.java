package com.example.hk.HK_Backend.service;

import com.example.hk.HK_Backend.dto.ApplicationRequest;
import com.example.hk.HK_Backend.dto.ApplicationResponse;
import com.example.hk.HK_Backend.dto.ApplicationStatusUpdateRequest;
import com.example.hk.HK_Backend.entity.*;
import com.example.hk.HK_Backend.exception.BadRequestException;
import com.example.hk.HK_Backend.exception.ResourceNotFoundException;
import com.example.hk.HK_Backend.repository.ApplicationRepository;
import com.example.hk.HK_Backend.repository.RoomRepository;
import com.example.hk.HK_Backend.repository.RoomTypeRepository;
import com.example.hk.HK_Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final RoomTypeRepository    roomTypeRepository;
    private final RoomRepository        roomRepository;
    private final UserRepository        userRepository;

    /**
     * Submit a new application.
     *
     * Business rules (FINAL):
     * 1. Find the room by room number (student manually entered R1-R9)
     * 2. Check the room is not full
     * 3. Save application with PENDING status
     * 4. DO NOT increment occupiedBeds yet — only admin CONFIRM does that
     *
     * Occupied beds only change when:
     *   - Admin CONFIRMS → increment
     *   - Admin REJECTS a CONFIRMED app → decrement
     *   - Admin DELETES a CONFIRMED app → decrement
     */
    @Transactional
    public ApplicationResponse submitApplication(ApplicationRequest req, String userEmail) {

        // Find room by room number
        Room assignedRoom = null;

        if (req.getPreferredRoomNumber() != null && !req.getPreferredRoomNumber().isBlank()) {
            assignedRoom = roomRepository.findByRoomNumber(req.getPreferredRoomNumber().trim())
                    .orElseThrow(() -> new BadRequestException(
                            "Room '" + req.getPreferredRoomNumber() + "' does not exist. Valid rooms: R1-R9."));

            // ── CRITICAL VALIDATION ───────────────────────────────────────────
            // If student came from "2 Sharing Book Now", roomTypeSlug = "2-sharing"
            // They must only be allowed to book a 2-sharing room.
            // If they typed R2 (4-sharing) by mistake → reject immediately.
            if (req.getRoomTypeSlug() != null && !req.getRoomTypeSlug().isBlank()) {
                String actualSlug   = assignedRoom.getRoomType().getSlug();
                String requestedSlug = req.getRoomTypeSlug();
                if (!actualSlug.equals(requestedSlug)) {
                    throw new BadRequestException(
                            "Room " + assignedRoom.getRoomNumber() + " is a " +
                            assignedRoom.getRoomType().getTitle() + " room, but you selected " +
                            requestedSlug + ". Please enter a room number that belongs to your selected room type.");
                }
            } else {
                // No roomTypeSlug sent — derive it from the room
                req.setRoomTypeSlug(assignedRoom.getRoomType().getSlug());
            }

            if (assignedRoom.isFull()) {
                throw new BadRequestException(
                        "Room " + assignedRoom.getRoomNumber() + " is fully occupied. Please choose a different room.");
            }

            // ── DUPLICATE BED CHECK ───────────────────────────────────────────
            // Prevent same bed from being booked twice (PENDING or CONFIRMED)
            if (req.getSelectedBedNumber() != null) {
                boolean bedAlreadyBooked = applicationRepository
                        .existsByRoomAndBedNumberAndStatusNot(
                                assignedRoom,
                                req.getSelectedBedNumber(),
                                ApplicationStatus.REJECTED);
                if (bedAlreadyBooked) {
                    throw new BadRequestException(
                            "Bed " + req.getSelectedBedNumber() + " in Room " + assignedRoom.getRoomNumber() +
                            " is already booked. Please select a different bed.");
                }
            }

            // Validate bed number is within this room's range
            if (req.getSelectedBedNumber() != null) {
                int bedStart = assignedRoom.getBedStart();
                int bedEnd   = bedStart + assignedRoom.getRoomType().getBedsPerRoom() - 1;
                if (req.getSelectedBedNumber() < bedStart || req.getSelectedBedNumber() > bedEnd) {
                    throw new BadRequestException(
                            "Bed " + req.getSelectedBedNumber() + " is not in Room " + assignedRoom.getRoomNumber() +
                            ". Valid beds for this room: " + bedStart + " to " + bedEnd + ".");
                }
            }
        }

        RoomType roomType = roomTypeRepository.findBySlug(req.getRoomTypeSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found: " + req.getRoomTypeSlug()));

        // Auto-assign first vacant room if none specified
        if (assignedRoom == null) {
            List<Room> vacantRooms = roomRepository.findVacantRoomsByType(roomType);
            if (vacantRooms.isEmpty()) {
                throw new BadRequestException(
                        "No vacant beds available in " + roomType.getTitle() + ". Please choose a different room type.");
            }
            assignedRoom = vacantRooms.get(0);
        }

        // NOTE: DO NOT increment occupiedBeds here.
        // Beds are only marked occupied when admin CONFIRMS the application.

        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        final Room finalRoom = assignedRoom;

        Application app = Application.builder()
                .user(user)
                .fullName(req.getFullName())
                .mobile(req.getMobile())
                .alternateMobile(req.getAlternateMobile())
                .email(req.getEmail())
                .address(req.getAddress())
                .city(req.getCity())
                .state(req.getState())
                .occupation(req.getOccupation())
                .institutionName(req.getInstitutionName())
                .courseOrRole(req.getCourseOrRole())
                .guardianName(req.getGuardianName())
                .guardianContact(req.getGuardianContact())
                .guardianRelation(req.getGuardianRelation())
                .joiningDate(req.getJoiningDate())
                .durationMonths(req.getDurationMonths())
                .roomType(roomType)
                .room(finalRoom)
                .bedNumber(req.getSelectedBedNumber())
                .depositAmount(req.getDepositAmount())
                .paymentMode(req.getPaymentMode())
                .transactionId(req.getTransactionId())
                .idProofType(req.getIdProofType())
                .status(ApplicationStatus.PENDING)
                .build();

        applicationRepository.save(app);
        log.info("Application submitted (PENDING): {} → {} (Bed {})", req.getFullName(), finalRoom.getRoomNumber(), req.getSelectedBedNumber());
        return toResponse(app);
    }

    /**
     * Admin updates application status.
     *
     * Bed count rules (FINAL):
     *   PENDING → CONFIRMED  : INCREMENT occupiedBeds (bed assigned)
     *   CONFIRMED → REJECTED : DECREMENT occupiedBeds (bed freed)
     *   PENDING → REJECTED   : NO change (bed was never occupied)
     *   REJECTED → PENDING   : NO change
     *   REJECTED → CONFIRMED : INCREMENT occupiedBeds
     */
    @Transactional
    public ApplicationResponse updateStatus(Long id, ApplicationStatusUpdateRequest req) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));

        ApplicationStatus oldStatus = app.getStatus();
        ApplicationStatus newStatus = req.getStatus();

        if (oldStatus == newStatus) {
            app.setAdminNotes(req.getAdminNotes());
            applicationRepository.save(app);
            return toResponse(app);
        }

        // CONFIRM: increment bed (only if not already confirmed)
        if (newStatus == ApplicationStatus.CONFIRMED && oldStatus != ApplicationStatus.CONFIRMED) {
            Room room = app.getRoom();
            if (room == null) {
                List<Room> vacantRooms = roomRepository.findVacantRoomsByType(app.getRoomType());
                if (vacantRooms.isEmpty()) {
                    throw new BadRequestException(
                            "No vacant beds in " + app.getRoomType().getTitle() + " to confirm this application.");
                }
                room = vacantRooms.get(0);
                app.setRoom(room);
            }
            if (room.isFull()) {
                throw new BadRequestException(
                        "Room " + room.getRoomNumber() + " is fully occupied. Cannot confirm.");
            }
            int updated = roomRepository.incrementOccupiedBeds(room.getId());
            if (updated == 0) {
                throw new BadRequestException("Room just became full. Please try again.");
            }
            log.info("Bed occupied: {} — application {} CONFIRMED", room.getRoomNumber(), id);
        }

        // REJECT a CONFIRMED app: decrement bed (free it)
        if (newStatus == ApplicationStatus.REJECTED && oldStatus == ApplicationStatus.CONFIRMED) {
            if (app.getRoom() != null) {
                roomRepository.decrementOccupiedBeds(app.getRoom().getId());
                log.info("Bed freed: {} — application {} REJECTED", app.getRoom().getRoomNumber(), id);
            }
        }

        // PENDING → REJECTED: no bed change (bed was never occupied)
        // REJECTED → PENDING: no bed change

        app.setStatus(newStatus);
        app.setAdminNotes(req.getAdminNotes());
        applicationRepository.save(app);
        return toResponse(app);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return applicationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(Long id) {
        return toResponse(applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id)));
    }

    @Transactional
    public void deleteApplication(Long id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));

        // Only free the bed if the application was CONFIRMED
        // (PENDING apps never occupied a bed)
        if (app.getStatus() == ApplicationStatus.CONFIRMED && app.getRoom() != null) {
            roomRepository.decrementOccupiedBeds(app.getRoom().getId());
            log.info("Bed freed on delete: {} (application {})", app.getRoom().getRoomNumber(), id);
        }

        applicationRepository.delete(app);
        log.info("Application {} deleted", id);
    }

    @Transactional
    public void updateIdProofUrl(Long id, String url) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
        app.setIdProofUrl(url);
        applicationRepository.save(app);
    }

    @Transactional
    public void updateProfilePhotoUrl(Long id, String url) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
        app.setProfilePhotoUrl(url);
        applicationRepository.save(app);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private ApplicationResponse toResponse(Application a) {
        return ApplicationResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .mobile(a.getMobile())
                .alternateMobile(a.getAlternateMobile())
                .email(a.getEmail())
                .address(a.getAddress())
                .city(a.getCity())
                .state(a.getState())
                .occupation(a.getOccupation())
                .institutionName(a.getInstitutionName())
                .courseOrRole(a.getCourseOrRole())
                .guardianName(a.getGuardianName())
                .guardianContact(a.getGuardianContact())
                .guardianRelation(a.getGuardianRelation())
                .joiningDate(a.getJoiningDate())
                .durationMonths(a.getDurationMonths())
                .roomTypeSlug(a.getRoomType() != null ? a.getRoomType().getSlug()  : null)
                .roomTypeTitle(a.getRoomType() != null ? a.getRoomType().getTitle() : null)
                .roomNumber(a.getRoom() != null ? a.getRoom().getRoomNumber() : null)
                .bedNumber(a.getBedNumber())   // stored directly — always correct
                .depositAmount(a.getDepositAmount())
                .paymentMode(a.getPaymentMode())
                .transactionId(a.getTransactionId())
                .idProofType(a.getIdProofType())
                .idProofUrl(a.getIdProofUrl())
                .profilePhotoUrl(a.getProfilePhotoUrl())
                .status(a.getStatus())
                .adminNotes(a.getAdminNotes())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
