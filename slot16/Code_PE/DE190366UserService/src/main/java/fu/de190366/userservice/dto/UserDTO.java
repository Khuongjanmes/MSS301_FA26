package fu.de190366.userservice.dto;

import fu.de190366.userservice.common.UserConstants;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
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
public class UserDTO {

    // Required-field checks only run on create; PUT is a partial update
    public interface OnCreate {}

    private Long userId;

    @NotBlank(groups = UserDTO.OnCreate.class, message = "Username is required")
    @Size(max = 30, message = "Username max length is 30")
    private String username;

    @NotBlank(groups = UserDTO.OnCreate.class, message = "Email is required")
    @Email(message = "Email is invalid")
    @Size(max = 100, message = "Email max length is 100")
    private String email;

    @NotBlank(groups = UserDTO.OnCreate.class, message = "Role is required")
    @Pattern(regexp = UserConstants.ROLE_REGEX, message = "Role must be USER, CREATOR, or ADMIN")
    private String role;

    @NotNull(groups = UserDTO.OnCreate.class, message = "ratePerPost is required")
    @DecimalMin(value = "0", inclusive = false, message = "ratePerPost must be > 0")
    private BigDecimal ratePerPost;

    @NotNull(groups = UserDTO.OnCreate.class, message = "maxDailyPosts is required")
    @Min(value = 1, message = "maxDailyPosts must be >= 1")
    @Max(value = 10, message = "maxDailyPosts must be <= 10")
    private Integer maxDailyPosts;

    @NotNull(groups = UserDTO.OnCreate.class, message = "level is required")
    @Min(value = 1, message = "level must be >= 1")
    private Integer level;

    // Optional on create (defaults to ACTIVE); validated only when present
    @Pattern(regexp = UserConstants.STATUS_REGEX, message = "status must be ACTIVE, SUSPENDED or DEACTIVATED")
    private String status;
}
