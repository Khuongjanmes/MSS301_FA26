package fu.de190366.reservation.client;

import fu.de190366.reservation.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "room-service", url = "${room.service.url}")
public interface RoomClient {

    @GetMapping("/api/rooms/{roomId}")
    ApiResponse getRoomById(@PathVariable("roomId") Long roomId);
}
