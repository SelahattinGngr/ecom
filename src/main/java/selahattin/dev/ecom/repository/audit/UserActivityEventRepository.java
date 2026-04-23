package selahattin.dev.ecom.repository.audit;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import selahattin.dev.ecom.entity.audit.UserActivityEventEntity;

public interface UserActivityEventRepository extends JpaRepository<UserActivityEventEntity, UUID> {
}
