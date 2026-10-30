package fu.de190366.reservation.service;

import fu.de190366.reservation.dto.ApiResponse;
import fu.de190366.reservation.dto.ReservationDTO;
import org.springframework.data.domain.Pageable;

public interface ReservationService {
    ApiResponse createReservation(ReservationDTO dto);
    ApiResponse updateReservation(Long reservationId, ReservationDTO dto);
    ApiResponse getReservationById(Long reservationId);
    ApiResponse cancelReservation(Long reservationId);
    ApiResponse getReservationList(String guestName, String status, Pageable pageable);
}
