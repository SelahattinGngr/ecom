package selahattin.dev.ecom.repository.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    // Müşteri tarafı
    List<OrderEntity> findAllByUserId(UUID userId);

    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<OrderEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // --- ADMIN TARAFI ---

    // Tüm siparişleri getir (Tarihe göre sıralı)
    Page<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Statüye göre filtrele (Örn: Sadece PENDING olanlar)
    Page<OrderEntity> findAllByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
}