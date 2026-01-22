package com.charbel.backend.repo;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.charbel.backend.model.InsightHistory;

public interface InsightHistoryRepo extends JpaRepository<InsightHistory, Long> {
    Optional<InsightHistory> findTopByUserIdAndFingerprintOrderByCreatedAtDesc(Long userId, String fingerprint);

    boolean existsByUserIdAndFingerprintAndCreatedAtAfter(Long userId, String fingerprint, LocalDateTime after);
}
