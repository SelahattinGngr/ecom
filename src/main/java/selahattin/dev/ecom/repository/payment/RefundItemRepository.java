package selahattin.dev.ecom.repository.payment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.payment.RefundItemEntity;

@Repository
public interface RefundItemRepository extends JpaRepository<RefundItemEntity, UUID> {

}
