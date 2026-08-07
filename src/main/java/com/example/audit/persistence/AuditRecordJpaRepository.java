package com.example.audit.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditRecordJpaRepository extends JpaRepository<AuditRecordEntity, Long> {
    List<AuditRecordEntity> findAllByOrderBySequenceAsc();
    List<AuditRecordEntity> findAllByArchivedFalseAndTimestampBefore(Instant threshold);
}
