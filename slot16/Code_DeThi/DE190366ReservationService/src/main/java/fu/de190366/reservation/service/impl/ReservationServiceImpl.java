package fu.de190366.reservation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import fu.de190366.reservation.client.RoomClient;
import fu.de190366.reservation.common.ReservationConstants;
import fu.de190366.reservation.dto.*;
import fu.de190366.reservation.entity.Reservation;
import fu.de190366.reservation.entity.ReservationStatus;
import fu.de190366.reservation.repository.ReservationRepository;
import fu.de190366.reservation.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomClient roomClient;
    private final ObjectMapper objectMapper;

    public ReservationServiceImpl(ReservationRepository reservationRepository, RoomClient roomClient,
                                  ObjectMapper objectMapper) {
        this.reservationRepository = reservationRepository;
        this.roomClient = roomClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ApiResponse createReservation(ReservationDTO dto) {
        // Date validation (status 2 -> 406)
        if (!dto.getCheckOutDate().after(dto.getCheckInDate())) {
            return ApiResponse.error(2, "checkOutDate must be after checkInDate");
        }

        // Fetch room from RoomService via OpenFeign
        RoomDTO room = fetchRoom(dto.getRoomId());
        if (room == null) {
            return ApiResponse.error(4, "Room ID is not found");   // 404
        }
        if (!ReservationConstants.ROOM_AVAILABLE.equals(room.getStatus())) {
            return ApiResponse.error(5, "Room is not AVAILABLE for reservation"); // 400
        }
        if (dto.getNumberOfGuests() > room.getCapacity()) {
            return ApiResponse.error(2, "numberOfGuests must not exceed the room capacity"); // 406
        }

        Reservation r = new Reservation();
        r.setGuestName(dto.getGuestName());
        r.setGuestEmail(dto.getGuestEmail());
        r.setGuestPhone(dto.getGuestPhone());
        r.setCheckInDate(dto.getCheckInDate());
        r.setCheckOutDate(dto.getCheckOutDate());
        r.setNumberOfGuests(dto.getNumberOfGuests());
        r.setRoomId(dto.getRoomId());
        r.setTotalAmount(computeTotal(room.getPricePerNight(), dto.getCheckInDate(), dto.getCheckOutDate()));
        r.setStatus(ReservationStatus.CONFIRMED); // default CONFIRMED

        Reservation saved = reservationRepository.save(r);
        return ApiResponse.of(1, "Reservation created successfully", toDTO(saved));
    }

    @Override
    @Transactional
    public ApiResponse updateReservation(Long reservationId, ReservationDTO dto) {
        Reservation r = reservationRepository.findById(reservationId).orElse(null);
        if (r == null) {
            return ApiResponse.error(4, "Reservation ID is not found");
        }
        // Validate the EFFECTIVE values BEFORE touching the managed entity, otherwise
        // dirty-checking would still flush bad values on commit (DB CHECK -> 500).
        Date effectiveIn  = dto.getCheckInDate()  != null ? dto.getCheckInDate()  : r.getCheckInDate();
        Date effectiveOut = dto.getCheckOutDate() != null ? dto.getCheckOutDate() : r.getCheckOutDate();
        if (!effectiveOut.after(effectiveIn)) {
            return ApiResponse.error(2, "checkOutDate must be after checkInDate");
        }
        // numberOfGuests must not exceed the room capacity (also checked before mutating)
        RoomDTO room = fetchRoom(r.getRoomId());
        Integer effectiveGuests = dto.getNumberOfGuests() != null ? dto.getNumberOfGuests() : r.getNumberOfGuests();
        if (room != null && effectiveGuests > room.getCapacity()) {
            return ApiResponse.error(2, "numberOfGuests must not exceed the room capacity");
        }

        // Partial update (safe now that values are validated)
        if (dto.getGuestName() != null)  r.setGuestName(dto.getGuestName());
        if (dto.getGuestEmail() != null) r.setGuestEmail(dto.getGuestEmail());
        if (dto.getGuestPhone() != null) r.setGuestPhone(dto.getGuestPhone());
        r.setCheckInDate(effectiveIn);
        r.setCheckOutDate(effectiveOut);
        if (dto.getNumberOfGuests() != null) r.setNumberOfGuests(dto.getNumberOfGuests());
        if (dto.getStatus() != null) r.setStatus(ReservationStatus.valueOf(dto.getStatus()));

        // Recompute total amount based on the current room price
        if (room != null) {
            r.setTotalAmount(computeTotal(room.getPricePerNight(), r.getCheckInDate(), r.getCheckOutDate()));
        }

        Reservation saved = reservationRepository.save(r);
        return ApiResponse.ok("Reservation updated successfully", toDTO(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getReservationById(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId).orElse(null);
        if (r == null) {
            return ApiResponse.error(4, "Reservation is not found");
        }
        return ApiResponse.ok("Successful", toDetailDTO(r));
    }

    @Override
    @Transactional
    public ApiResponse cancelReservation(Long reservationId) {
        Reservation r = reservationRepository.findById(reservationId).orElse(null);
        if (r == null) {
            return ApiResponse.error(4, "Reservation is not found");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(r);
        return ApiResponse.ok("Reservation cancelled successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse getReservationList(String guestName, String status, Pageable pageable) {
        Page<Reservation> page = reservationRepository.search(emptyToNull(guestName), parseStatus(status), pageable);
        List<ReservationDetailDTO> content = page.getContent().stream().map(this::toDetailDTO).toList();

        PageDTO pageDTO = new PageDTO(
                page.getSize(), page.getNumber(), page.getTotalPages(),
                page.getTotalElements(), page.isFirst(), page.isLast(), content);
        return ApiResponse.ok("Successful", pageDTO);
    }

    // -- helpers --

    /** Goi RoomService qua OpenFeign; tra null khi room khong ton tai (HTTP 404). */
    private RoomDTO fetchRoom(Long roomId) {
        try {
            ApiResponse res = roomClient.getRoomById(roomId);
            if (res == null || res.getStatus() != 1 || res.getData() == null) {
                return null;
            }
            return objectMapper.convertValue(res.getData(), RoomDTO.class);
        } catch (FeignException.NotFound ex) {
            // 404 tu RoomService -> coi nhu khong tim thay room (status 4)
            return null;
        }
        // Cac FeignException khac (503, timeout...) se duoc nem tiep
        // va GlobalExceptionHandler tra 500/status=0.
    }

    // totalAmount = pricePerNight x number of nights
    private BigDecimal computeTotal(BigDecimal pricePerNight, Date checkIn, Date checkOut) {
        long nights = ChronoUnit.DAYS.between(toLocalDate(checkIn), toLocalDate(checkOut));
        if (nights < 1) nights = 1;
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }

    // Use getTime(): java.sql.Date loaded by Hibernate throws on toInstant()
    private java.time.LocalDate toLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private ReservationDTO toDTO(Reservation r) {
        ReservationDTO d = new ReservationDTO();
        d.setReservationId(r.getReservationId());
        d.setGuestName(r.getGuestName());
        d.setGuestEmail(r.getGuestEmail());
        d.setGuestPhone(r.getGuestPhone());
        d.setCheckInDate(r.getCheckInDate());
        d.setCheckOutDate(r.getCheckOutDate());
        d.setNumberOfGuests(r.getNumberOfGuests());
        d.setTotalAmount(r.getTotalAmount());
        d.setStatus(r.getStatus().name());
        d.setRoomId(r.getRoomId());
        return d;
    }

    private ReservationDetailDTO toDetailDTO(Reservation r) {
        ReservationDetailDTO d = new ReservationDetailDTO();
        d.setReservationId(r.getReservationId());
        d.setGuestName(r.getGuestName());
        d.setGuestEmail(r.getGuestEmail());
        d.setGuestPhone(r.getGuestPhone());
        d.setCheckInDate(r.getCheckInDate());
        d.setCheckOutDate(r.getCheckOutDate());
        d.setNumberOfGuests(r.getNumberOfGuests());
        d.setTotalAmount(r.getTotalAmount());
        d.setStatus(r.getStatus().name());
        d.setRoom(fetchRoom(r.getRoomId())); // may be null if room removed
        return d;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // Parse filter string -> enum (null when empty); invalid values are rejected by the controller
    private ReservationStatus parseStatus(String s) {
        return (s == null || s.isBlank()) ? null : ReservationStatus.valueOf(s);
    }
}
