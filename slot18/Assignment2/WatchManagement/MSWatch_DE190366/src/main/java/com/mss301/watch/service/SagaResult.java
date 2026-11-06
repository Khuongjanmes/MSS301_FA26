package com.mss301.watch.service;

/** Kết quả trả cho controller: sagaId (để đặt header X-Saga-Id) + body. */
// [TODO-6.14]
public record SagaResult<T>(String sagaId, T body) {
}
