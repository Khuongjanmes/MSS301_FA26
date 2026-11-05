package com.mss301.watch.service.saga;

/** Kết quả của 1 Saga chạy thành công (thất bại thì orchestrator ném SagaExecutionException). */
// [TODO-6.11]
public record SagaOutcome(String sagaId, Integer watchId, SagaStatus status) {
}
