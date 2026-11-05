package com.mss301.brand.dto;

/**
 * Kết quả của 1 bước Saga ở MSBrand.
 *
 * @param watchId         watch được gán / gỡ
 * @param brandId         brand hiện tại sau thao tác (null nếu đã gỡ)
 * @param previousBrandId brand trước thao tác (null nếu trước đó chưa gán)
 * @param result          ASSIGNED | UNASSIGNED | NOTHING_TO_DO
 */
// [TODO-5.4]
public record AssignmentResponse(Integer watchId, Integer brandId, Integer previousBrandId, String result) {
}
