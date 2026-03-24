package selahattin.dev.ecom.repository.payment;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.payment.RefundEntity;

@Repository
public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {

    @Query("SELECT r FROM RefundEntity r WHERE r.payment.order.user.id = :userId ORDER BY r.createdAt DESC")
    Page<RefundEntity> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT r FROM RefundEntity r WHERE r.id = :id AND r.payment.order.user.id = :userId")
    Optional<RefundEntity> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT r FROM RefundEntity r ORDER BY r.createdAt DESC")
    Page<RefundEntity> findAllOrderByCreatedAtDesc(Pageable pageable);
}
