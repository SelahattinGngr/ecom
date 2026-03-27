package selahattin.dev.ecom.repository.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.payment.PaymentEventEntity;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEventEntity, UUID> {

    List<PaymentEventEntity> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
}
