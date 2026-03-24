package selahattin.dev.ecom.repository.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.order.OrderItemEntity;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {

    @Query(value = """
            SELECT oi.product_name_at_purchase,
                   SUM(oi.quantity) AS total_qty,
                   SUM(oi.quantity * oi.price_at_purchase) AS revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.created_at >= :from AND o.created_at < :to
              AND o.status != 'CANCELLED'
            GROUP BY oi.product_name_at_purchase
            ORDER BY total_qty DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findTopProductsByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItemEntity oi WHERE oi.order.createdAt >= :from AND oi.order.createdAt < :to AND oi.order.status != selahattin.dev.ecom.utils.enums.OrderStatus.CANCELLED")
    Long sumItemsSoldByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(oi.quantity * oi.priceAtPurchase), 0) FROM OrderItemEntity oi WHERE oi.order.createdAt >= :from AND oi.order.createdAt < :to AND oi.order.status != selahattin.dev.ecom.utils.enums.OrderStatus.CANCELLED")
    java.math.BigDecimal sumProductRevenueByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
