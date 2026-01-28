package selahattin.dev.ecom.repository.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import selahattin.dev.ecom.entity.order.OrderEntity;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findAllByUserId(UUID userId);

    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);
}