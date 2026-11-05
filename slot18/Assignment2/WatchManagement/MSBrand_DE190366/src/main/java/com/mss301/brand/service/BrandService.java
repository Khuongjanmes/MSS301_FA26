package com.mss301.brand.service;

import com.mss301.brand.dto.AssignmentResponse;
import com.mss301.brand.entity.BrandWatch;
import com.mss301.brand.repository.BrandRepository;
import com.mss301.brand.repository.BrandWatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

// [TODO-5.2, TODO-5.3: local transaction + compensation idempotent]
@Service
@RequiredArgsConstructor
@Slf4j
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandWatchRepository brandWatchRepository;

    // ============ Internal API cho Saga (LOCAL TRANSACTION trên DBBrand) ============

    /**
     * Gán watch vào brand (upsert theo WatchID).
     * - Brand không tồn tại -> 404 => MSWatch hiểu là bước Saga thất bại và sẽ compensate.
     * - IDEMPOTENT: gọi lại nhiều lần với cùng (brandId, watchId) cho cùng một kết quả.
     * - Dùng cho: CREATE (gán mới), UPDATE (chuyển brand), compensation của DELETE/UPDATE (gán lại brand cũ).
     */
    // [TODO-5.2: gán watch (upsert, idempotent)]
    @Transactional
    public AssignmentResponse assignWatch(Integer brandId, Integer watchId) {
        if (!brandRepository.existsById(brandId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found with id: " + brandId);
        }
        BrandWatch link = brandWatchRepository.findByWatchId(watchId)
            .orElseGet(() -> BrandWatch.builder().watchId(watchId).build());
        Integer previous = link.getBrandId();

        link.setBrandId(brandId);
        link.setUpdatedAt(LocalDateTime.now());
        brandWatchRepository.save(link);

        log.info("[SAGA-PARTICIPANT] ASSIGN watch={} -> brand={} (previous brand={})", watchId, brandId, previous);
        return new AssignmentResponse(watchId, brandId, previous, "ASSIGNED");
    }

    /**
     * Gỡ watch khỏi brand.
     * - IDEMPOTENT: watch chưa từng gán / đã gỡ rồi -> vẫn trả 200 NOTHING_TO_DO, không lỗi.
     * - Dùng cho: DELETE (bước 1) và compensation của CREATE.
     */
    // [TODO-5.3: gỡ watch (idempotent)]
    @Transactional
    public AssignmentResponse unassignWatch(Integer watchId) {
        Optional<BrandWatch> existing = brandWatchRepository.findByWatchId(watchId);
        if (existing.isEmpty()) {
            log.info("[SAGA-PARTICIPANT] UNASSIGN watch={} -> nothing to do (idempotent)", watchId);
            return new AssignmentResponse(watchId, null, null, "NOTHING_TO_DO");
        }
        Integer previous = existing.get().getBrandId();
        brandWatchRepository.delete(existing.get());
        log.info("[SAGA-PARTICIPANT] UNASSIGN watch={} from brand={}", watchId, previous);
        return new AssignmentResponse(watchId, null, previous, "UNASSIGNED");
    }
}
