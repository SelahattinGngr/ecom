package selahattin.dev.ecom.repository.audit;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.audit.AdminAuditLogEntity;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, UUID> {

    @Query("""
            SELECT a FROM AdminAuditLogEntity a
            WHERE (:userId IS NULL OR a.user.id = :userId)
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:action IS NULL OR a.action = :action)
            ORDER BY a.createdAt DESC
            """)
    Page<AdminAuditLogEntity> findWithFilters(
            @Param("userId") UUID userId,
            @Param("entityType") String entityType,
            @Param("action") String action,
            Pageable pageable);
}
