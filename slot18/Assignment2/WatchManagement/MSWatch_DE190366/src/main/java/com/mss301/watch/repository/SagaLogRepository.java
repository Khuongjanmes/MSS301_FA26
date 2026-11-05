package com.mss301.watch.repository;

import com.mss301.watch.entity.SagaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// [TODO-6.2]
public interface SagaLogRepository extends JpaRepository<SagaLog, Long> {

    /** Các bước theo đúng thứ tự đã xảy ra. */
    List<SagaLog> findBySagaIdOrderByIdAsc(String sagaId);
}
