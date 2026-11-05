package com.mss301.watch.controller;

import com.mss301.watch.dto.ApiResponse;
import com.mss301.watch.dto.SagaResponse;
import com.mss301.watch.entity.SagaState;
import com.mss301.watch.repository.SagaLogRepository;
import com.mss301.watch.repository.SagaStateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Xem trạng thái + nhật ký Saga để CHỨNG MINH Saga đã chạy / bù trừ thế nào (Admin only, xem AdminRoleInterceptor). */
// [TODO-6.14: GET /api/sagas, /api/sagas/{sagaId}]
@RestController
@RequestMapping("/api/sagas")
@RequiredArgsConstructor
@Tag(name = "Saga", description = "Saga state & step logs (for testing/monitoring)")
public class SagaController {

    private final SagaStateRepository stateRepository;
    private final SagaLogRepository logRepository;

    @GetMapping
    @Operation(summary = "Các saga gần nhất (mới nhất trước)")
    public ResponseEntity<ApiResponse<List<SagaResponse>>> recent(@RequestParam(defaultValue = "20") int limit) {
        int size = Math.clamp(limit, 1, 100);       // limit <= 0 hoặc quá lớn -> vẫn hợp lệ, không 500
        List<SagaResponse> list = stateRepository.findAllByOrderByIdDesc(PageRequest.of(0, size))
            .stream().map(s -> SagaResponse.of(s, null)).toList();
        return ResponseEntity.ok(ApiResponse.success(list, "Success"));
    }

    @GetMapping("/{sagaId}")
    @Operation(summary = "Chi tiết 1 saga kèm từng bước EXECUTE / COMPENSATE")
    public ResponseEntity<ApiResponse<SagaResponse>> detail(@PathVariable String sagaId) {
        SagaState state = stateRepository.findBySagaId(sagaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Saga not found: " + sagaId));
        return ResponseEntity.ok(ApiResponse.success(
            SagaResponse.of(state, logRepository.findBySagaIdOrderByIdAsc(sagaId)), "Success"));
    }
}
