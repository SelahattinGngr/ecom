package selahattin.dev.ecom.repository.payment;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    // Admin listesi için tarihe göre sıralı getirme
    Page<PaymentEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<PaymentEntity> findByPaymentTransactionId(String paymentTransactionId);

    // Siparişin başarılı ödemesini bul (iade için)
    Optional<PaymentEntity> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);

}