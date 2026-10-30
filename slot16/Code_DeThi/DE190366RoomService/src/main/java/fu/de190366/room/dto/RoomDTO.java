package fu.de190366.room.dto;

import fu.de190366.room.common.RoomConstants;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RoomDTO {

    // Required-field checks only run on create; PUT is a partial update
    public interface OnCreate {}

    private Long roomId;

    @NotBlank(groups = RoomDTO.OnCreate.class, message = "roomNumber is required")
    @Pattern(regexp = ".*\\S.*", message = "roomNumber must not be blank")
    @Size(max = 20, message = "roomNumber max length is 20")
    private String roomNumber;

    @NotBlank(groups = RoomDTO.OnCreate.class, message = "roomType is required")
    @Pattern(regexp = RoomConstants.ROOM_TYPE_REGEX, message = "roomType must be SINGLE, DOUBLE, SUITE or DELUXE")
    private String roomType;

    @NotNull(groups = RoomDTO.OnCreate.class, message = "pricePerNight is required")
    @DecimalMin(value = "0", inclusive = false, message = "pricePerNight must be > 0")
    @Digits(integer = 16, fraction = 2, message = "pricePerNight must fit decimal(18,2)")
    private BigDecimal pricePerNight;

    @NotNull(groups = RoomDTO.OnCreate.class, message = "capacity is required")
    @Min(value = 1, message = "capacity must be >= 1")
    @Max(value = 10, message = "capacity must be <= 10")
    private Integer capacity;

    @NotNull(groups = RoomDTO.OnCreate.class, message = "floor is required")
    @Min(value = 1, message = "floor must be >= 1")
    private Integer floor;

    // Optional on create (defaults to AVAILABLE); validated only when present
    @Pattern(regexp = RoomConstants.STATUS_REGEX, message = "status must be AVAILABLE, OCCUPIED or MAINTENANCE")
    private String status;
}
