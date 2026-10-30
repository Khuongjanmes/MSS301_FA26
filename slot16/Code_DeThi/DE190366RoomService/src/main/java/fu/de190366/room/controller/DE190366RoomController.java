package fu.de190366.room.controller;

import fu.de190366.room.common.RoomConstants;
import fu.de190366.room.dto.ApiResponse;
import fu.de190366.room.dto.RoomDTO;
import fu.de190366.room.service.RoomService;
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
@RequestMapping("/api/rooms")
public class DE190366RoomController {
    private final RoomService roomService;

    public DE190366RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // 1. Create Room -> 201/1, 406/2, 226/3, 500/0
    @PostMapping
    public ResponseEntity<ApiResponse> create(@Validated({RoomDTO.OnCreate.class, Default.class}) @RequestBody RoomDTO roomDTO,
                                              BindingResult br) {
        if (br.hasErrors()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, firstError(br)));
        }
        return map(roomService.createRoom(roomDTO), HttpStatus.CREATED); // 201 on success
    }

    // 2. Update Room -> 200/1, 406/2, 226/3, 404/4, 500/0
    @PutMapping("/{roomId}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long roomId,
                                              @Valid @RequestBody RoomDTO roomDTO,
                                              BindingResult br) {
        if (br.hasErrors()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, firstError(br)));
        }
        return map(roomService.updateRoom(roomId, roomDTO), HttpStatus.OK);
    }

    // 3. Get Room detail -> 200/1, 404/4, 500/0
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse> getDetail(@PathVariable Long roomId) {
        return map(roomService.getRoomById(roomId), HttpStatus.OK);
    }

    // 4. Set Room to MAINTENANCE (delete) -> 200/1, 404/4, 500/0
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse> setMaintenance(@PathVariable Long roomId) {
        return map(roomService.setMaintenance(roomId), HttpStatus.OK);
    }

    // 5. Get Room List -> 200/1, 406/2, 500/0
    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String roomType,
                                            @RequestParam(required = false) String status) {
        if (size < 1 || size > 100) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, "size must be between 1 and 100"));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, "page must be >= 0"));
        }
        if (roomType != null && !roomType.isBlank() && !roomType.matches(RoomConstants.ROOM_TYPE_REGEX)) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, "roomType must be SINGLE, DOUBLE, SUITE or DELUXE"));
        }
        if (status != null && !status.isBlank() && !status.matches(RoomConstants.STATUS_REGEX)) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, "status must be AVAILABLE, OCCUPIED or MAINTENANCE"));
        }
        Pageable pageable = PageRequest.of(page, size);
        return map(roomService.getRoomList(roomType, status, pageable), HttpStatus.OK);
    }

    private String firstError(BindingResult br) {
        return br.getAllErrors().isEmpty() ? "Validation failed" : br.getAllErrors().get(0).getDefaultMessage();
    }

    // map service status -> HTTP code
    private ResponseEntity<ApiResponse> map(ApiResponse apiResponse, HttpStatus okStatus) {
        HttpStatus httpStatus = switch (apiResponse.getStatus()) {
            case 1 -> okStatus;                         // success
            case 2 -> HttpStatus.NOT_ACCEPTABLE;        // 406 validation error
            case 3 -> HttpStatus.IM_USED;               // 226 duplicated room number
            case 4 -> HttpStatus.NOT_FOUND;             // 404 room not found
            default -> HttpStatus.INTERNAL_SERVER_ERROR; // 500 unknown error
        };
        return ResponseEntity.status(httpStatus).body(apiResponse);
    }
}
