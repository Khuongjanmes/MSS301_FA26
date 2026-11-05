package com.mss301.watch.exception;

import com.mss301.watch.dto.ApiResponse;
import com.mss301.watch.service.saga.SagaExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// [TODO-6.15: chuyển SagaExecutionException / validation thành HTTP response]
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Saga thất bại: trả về sagaId + trạng thái cuối để client tra cứu GET /api/sagas/{sagaId}. */
    @ExceptionHandler(SagaExecutionException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleSaga(SagaExecutionException ex) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sagaId", ex.getSagaId());
        data.put("sagaStatus", ex.getSagaStatus());
        data.put("failedStep", ex.getFailedStep());
        data.put("reason", ex.getReason());
        return ResponseEntity.status(ex.getHttpStatus())
            .header("X-Saga-Id", ex.getSagaId())
            .body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Saga " + ex.getSagaStatus() + ": " + ex.getReason())
                .data(data)
                .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
            .body(ApiResponse.error(ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    /** Body không phải JSON hợp lệ, hoặc sai kiểu (VD: releaseDate "abc") -> 400, không phải 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Malformed request body"));
    }

    /** VD: /api/watches/abc hoặc header X-Saga-Fail-After-Step: abc -> 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Invalid value for '" + ex.getName() + "': " + ex.getValue()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("Resource not found: /" + ex.getResourcePath()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error("Method " + ex.getMethod() + " not supported"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        // Ghi chi tiết vào log, không trả message nội bộ (SQL, stack...) ra ngoài
        log.error("Unhandled error", ex);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("Internal server error"));
    }
}
