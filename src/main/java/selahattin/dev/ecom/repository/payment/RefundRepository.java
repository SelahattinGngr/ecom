package selahattin.dev.ecom.repository.payment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.payment.RefundEntity;

@Repository
public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {
    
}
