package fu.de190366.userservice.controller;

import fu.de190366.userservice.dto.ApiResponse;
import fu.de190366.userservice.dto.UserDTO;
import fu.de190366.userservice.service.UserService;
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
@RequestMapping("/api/users")
public class DE190366UserController {
    private final UserService userService;

    public DE190366UserController(UserService userService) {
        this.userService = userService;
    }

    // 1. Create User -> 201/1, 406/2, 226/3, 500/0
    @PostMapping
    public ResponseEntity<ApiResponse> create(@Validated({UserDTO.OnCreate.class, Default.class}) @RequestBody UserDTO userDTO,
                                              BindingResult br) {
        if (br.hasErrors()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, firstError(br)));
        }
        return map(userService.createUser(userDTO), HttpStatus.CREATED); // 201 on success
    }

    // 2. Update User -> 200/1, 406/2, 226/3, 404/4, 500/0
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long userId,
                                              @Valid @RequestBody UserDTO userDTO,
                                              BindingResult br) {
        if (br.hasErrors()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, firstError(br)));
        }
        return map(userService.updateUser(userId, userDTO), HttpStatus.OK); // 200 on success
    }

    // 3. Get User detail -> 200/1, 404/4, 500/0
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getDetail(@PathVariable Long userId) {
        return map(userService.getUserById(userId), HttpStatus.OK);
    }

    // 4. Set to DEACTIVATED (delete) -> 200/1, 404/4, 500/0
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long userId) {
        return map(userService.deactivateUser(userId), HttpStatus.OK);
    }

    // 5. Get User List -> 200/1, 406/2, 500/0
    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestParam(required = false) String role,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        if (size < 1 || size > 100) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, "size must be between 1 and 100"));
        }
        if (page < 0) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE) // 406
                    .body(ApiResponse.error(2, "page must be >= 0"));
        }
        Pageable pageable = PageRequest.of(page, size);
        return map(userService.getUserList(role, status, pageable), HttpStatus.OK);
    }

    private String firstError(BindingResult br) {
        return br.getAllErrors().isEmpty() ? "Validation failed" : br.getAllErrors().get(0).getDefaultMessage();
    }

    // map service status -> HTTP code
    private ResponseEntity<ApiResponse> map(ApiResponse apiResponse, HttpStatus okStatus) {
        HttpStatus httpStatus = switch (apiResponse.getStatus()) {
            case 1 -> okStatus;                         // success
            case 2 -> HttpStatus.NOT_ACCEPTABLE;        // 406 validation error
            case 3 -> HttpStatus.IM_USED;               // 226 duplicated username
            case 4 -> HttpStatus.NOT_FOUND;             // 404 user not found
            default -> HttpStatus.INTERNAL_SERVER_ERROR; // 500 unknown error
        };
        return ResponseEntity.status(httpStatus).body(apiResponse);
    }
}
