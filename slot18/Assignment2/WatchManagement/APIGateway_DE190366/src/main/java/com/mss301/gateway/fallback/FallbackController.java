package com.mss301.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback khi CircuitBreaker OPEN hoặc service không phản hồi (timeout / mất kết nối).
 * Chỉ TRẢ LỖI 503 nhanh, không ghi dữ liệu gì -> không ảnh hưởng tính nhất quán của Saga.
 */
// [TODO-7.2: fallback khi CircuitBreaker OPEN]
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/account")
    public ResponseEntity<Map<String, Object>> accountFallback() {
        return unavailable("Account Service");
    }

    @RequestMapping("/brand")
    public ResponseEntity<Map<String, Object>> brandFallback() {
        return unavailable("Brand Service");
    }

    @RequestMapping("/watch")
    public ResponseEntity<Map<String, Object>> watchFallback() {
        return unavailable("Watch Service");
    }

    private ResponseEntity<Map<String, Object>> unavailable(String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "success", false,
            "message", service + " is currently unavailable. Please try again later.",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
