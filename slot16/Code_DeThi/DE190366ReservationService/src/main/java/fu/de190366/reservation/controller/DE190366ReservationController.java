package fu.de190366.reservation.controller;

import fu.de190366.reservation.common.ReservationConstants;
import fu.de190366.reservation.dto.ApiResponse;
import fu.de190366.reservation.dto.ReservationDTO;
import fu.de190366.reservation.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class DE190366ReservationController {

    private final ReservationService reservationService;

    public DE190366ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // 1. Create Reservation -> 201/1, 406/2, 404/4, 400/5, 500/0
    @PostMapping
    public ResponseEntity<ApiResponse> create(@Validated({ReservationDTO.OnCreate.class, Default.class}) @RequestBody ReservationDTO dto,
                                              BindingResult br) {
        if (br.hasErrors()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, firstError(br)));
        }
        return map(reservationService.createReservation(dto), HttpStatus.CREATED);
    }

    // 2. Update Reservation -> 200/1, 406/2, 404/4, 500/0
    @PutMapping("/{reservationId}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long reservationId,
                                              @Valid @RequestBody ReservationDTO dto,
                                              BindingResult br) {
        if (br.hasErrors()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, firstError(br)));
        }
        return map(reservationService.updateReservation(reservationId, dto), HttpStatus.OK);
    }

    // 3. Get Reservation Detail -> 200/1, 404/4, 500/0
    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long reservationId) {
        return map(reservationService.getReservationById(reservationId), HttpStatus.OK);
    }

    // 4. Cancel Reservation -> 200/1, 404/4, 500/0
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ApiResponse> cancel(@PathVariable Long reservationId) {
        return map(reservationService.cancelReservation(reservationId), HttpStatus.OK);
    }

    // 5. Get Reservation List -> 200/1, 406/2, 500/0
    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String guestName,
                                            @RequestParam(required = false) String status) {
        if (size < 1 || size > 100) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "size must be between 1 and 100"));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "page must be >= 0"));
        }
        if (status != null && !status.isBlank() && !status.matches(ReservationConstants.STATUS_REGEX)) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "status must be CONFIRMED, CHECKED_IN, CHECKED_OUT or CANCELLED"));
        }
        Pageable pageable = PageRequest.of(page, size);
        return map(reservationService.getReservationList(guestName, status, pageable), HttpStatus.OK);
    }

    // -- map service status -> HTTP code --
    private ResponseEntity<ApiResponse> map(ApiResponse res, HttpStatus okStatus) {
        return switch (res.getStatus()) {
            case 1 -> ResponseEntity.status(okStatus).body(res);
            case 2 -> ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(res);      // 406
            case 4 -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);           // 404
            case 5 -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);         // 400 room not available
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        };
    }

    private String firstError(BindingResult br) {
        return br.getAllErrors().isEmpty() ? "Validation failed"
                : br.getAllErrors().get(0).getDefaultMessage();
    }
}
