package selahattin.dev.ecom.repository.audit;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.audit.AdminAuditLogEntity;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, UUID> {

}
