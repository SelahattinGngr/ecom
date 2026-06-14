package selahattin.dev.ecom.repository.payment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    // Admin listesi için tarihe göre sıralı getirme
    Page<PaymentEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<PaymentEntity> findByPaymentTransactionId(String paymentTransactionId);

    // Siparişin başarılı ödemesini bul (iade için)
    Optional<PaymentEntity> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    // Çift iade (double refund) önlemi: pessimistic write lock ile okuma.
    // Aynı anda iki istek gelirse biri lock'u bekler; lock alan işlem durumu REFUNDED
    // yaptıktan sonra diğeri SUCCEEDED kaydı bulamaz ve doğal olarak durur.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.order.id = :orderId AND p.status = :status")
    Optional<PaymentEntity> findByOrderIdAndStatusForUpdate(
            @Param("orderId") UUID orderId,
            @Param("status") PaymentStatus status);

    // Siparişe ait en güncel ödemeyi getir
    Optional<PaymentEntity> findTopByOrderIdOrderByCreatedAtDesc(UUID orderId);

    // --- PERFORMANCE: N+1 FIX ---

    @Query(value = "SELECT p FROM PaymentEntity p JOIN FETCH p.order o JOIN FETCH o.user",
           countQuery = "SELECT COUNT(p) FROM PaymentEntity p")
    Page<PaymentEntity> findAllWithOrderAndUser(Pageable pageable);

    // --- ANALİTİK ---

    @Query("SELECT COUNT(p) FROM PaymentEntity p WHERE p.createdAt >= :from AND p.createdAt < :to")
    Long countByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.status = 'SUCCEEDED' AND p.createdAt >= :from AND p.createdAt < :to")
    java.math.BigDecimal sumRevenueByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.status = 'REFUNDED' AND p.createdAt >= :from AND p.createdAt < :to")
    java.math.BigDecimal sumRefundedByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT p.status, COUNT(p) FROM PaymentEntity p WHERE p.createdAt >= :from AND p.createdAt < :to GROUP BY p.status")
    List<Object[]> countByStatusAndPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT p.paymentProvider, COUNT(p), COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.createdAt >= :from AND p.createdAt < :to GROUP BY p.paymentProvider")
    List<Object[]> countAndRevenueByProviderAndPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}