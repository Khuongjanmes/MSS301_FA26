package fu.de190366.room.service;

import fu.de190366.room.dto.ApiResponse;
import fu.de190366.room.dto.RoomDTO;
import org.springframework.data.domain.Pageable;

public interface RoomService {
    ApiResponse createRoom(RoomDTO dto);
    ApiResponse updateRoom(Long roomId, RoomDTO dto);
    ApiResponse getRoomById(Long roomId);
    ApiResponse setMaintenance(Long roomId);
    ApiResponse getRoomList(String roomType, String status, Pageable pageable);
}
