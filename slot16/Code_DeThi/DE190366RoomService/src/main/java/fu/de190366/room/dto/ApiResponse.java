package fu.de190366.room.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Getter
@Setter
@NoArgsConstructor
public class ApiResponse {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private int status;
    private String message;
    private Object data;
    private String timestamp;

    public ApiResponse(int status, String message, Object data, String timestamp) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    public static ApiResponse of(int status, String message, Object data) {
        return new ApiResponse(status, message, data, ISO.format(Instant.now()));
    }

    public static ApiResponse ok(String message, Object data) {
        return of(1, message, data);
    }

    public static ApiResponse error(int status, String message) {
        return of(status, message, null);
    }
}
