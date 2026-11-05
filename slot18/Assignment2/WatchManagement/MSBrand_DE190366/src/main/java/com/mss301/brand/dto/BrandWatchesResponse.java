package com.mss301.brand.dto;

import java.util.List;

/** {brandId, brandName, watchCount, watchIds[]} - dùng để kiểm chứng Saga. */
// [TODO-5.5]
public record BrandWatchesResponse(Integer brandId, String brandName, int watchCount, List<Integer> watchIds) {
}
