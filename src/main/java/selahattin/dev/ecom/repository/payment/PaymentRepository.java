package selahattin.dev.ecom.repository.payment;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.payment.PaymentEntity;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    // Admin listesi için tarihe göre sıralı getirme
    Page<PaymentEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}