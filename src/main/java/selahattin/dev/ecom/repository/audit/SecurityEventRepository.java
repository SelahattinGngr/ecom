package selahattin.dev.ecom.repository.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.audit.SecurityEventEntity;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEventEntity, UUID> {

    @Query("""
            SELECT s FROM SecurityEventEntity s
            WHERE (:userId IS NULL OR s.userId = :userId)
              AND (:eventType IS NULL OR s.eventType = :eventType)
              AND (:from IS NULL OR s.createdAt >= :from)
              AND (:to IS NULL OR s.createdAt <= :to)
            ORDER BY s.createdAt DESC
            """)
    Page<SecurityEventEntity> findWithFilters(
            @Param("userId") UUID userId,
            @Param("eventType") String eventType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);

    long countByUserIdAndEventTypeAndCreatedAtAfter(UUID userId, String eventType, OffsetDateTime after);
}
