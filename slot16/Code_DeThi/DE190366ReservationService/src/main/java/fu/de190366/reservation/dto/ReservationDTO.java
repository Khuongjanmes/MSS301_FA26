package fu.de190366.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.de190366.reservation.common.ReservationConstants;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Date;

public class ReservationDTO {

    // Required-field checks only run on create; PUT is a partial update
    public interface OnCreate {}

    private Long reservationId;

    @NotBlank(groups = ReservationDTO.OnCreate.class, message = "guestName is required")
    @Pattern(regexp = ".*\\S.*", message = "guestName must not be blank")
    @Size(max = 100, message = "guestName max length is 100")
    private String guestName;

    @NotBlank(groups = ReservationDTO.OnCreate.class, message = "guestEmail is required")
    @Email(message = "guestEmail is invalid")
    @Size(max = 100, message = "guestEmail max length is 100")
    private String guestEmail;

    @NotBlank(groups = ReservationDTO.OnCreate.class, message = "guestPhone is required")
    @Pattern(regexp = ".*\\S.*", message = "guestPhone must not be blank")
    @Size(max = 20, message = "guestPhone max length is 20")
    private String guestPhone;

    @NotNull(groups = ReservationDTO.OnCreate.class, message = "checkInDate is required")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date checkInDate;

    @NotNull(groups = ReservationDTO.OnCreate.class, message = "checkOutDate is required")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private Date checkOutDate;

    @NotNull(groups = ReservationDTO.OnCreate.class, message = "numberOfGuests is required")
    @Min(value = 1, message = "numberOfGuests must be >= 1")
    @Max(value = 10, message = "numberOfGuests must be <= 10")
    private Integer numberOfGuests;

    // Computed by the server = pricePerNight x number of nights
    private BigDecimal totalAmount;

    // Optional; defaults to CONFIRMED on create
    @Pattern(regexp = ReservationConstants.STATUS_REGEX,
            message = "status must be CONFIRMED, CHECKED_IN, CHECKED_OUT or CANCELLED")
    private String status;

    @NotNull(groups = ReservationDTO.OnCreate.class, message = "roomId is required")
    private Long roomId;

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }
    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }
    public Date getCheckInDate() { return checkInDate; }
    public void setCheckInDate(Date checkInDate) { this.checkInDate = checkInDate; }
    public Date getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(Date checkOutDate) { this.checkOutDate = checkOutDate; }
    public Integer getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(Integer numberOfGuests) { this.numberOfGuests = numberOfGuests; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
}
