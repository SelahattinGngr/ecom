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
            SELECT pv.product_id,
                   MAX(oi.product_name_at_purchase) AS product_name,
                   SUM(oi.quantity) AS total_qty
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            JOIN product_variants pv ON oi.product_variant_id = pv.id
            WHERE o.created_at >= :from AND o.created_at < :to
              AND o.status != 'CANCELLED'::order_status
            GROUP BY pv.product_id
            ORDER BY total_qty DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findTopProductsByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT c.id AS category_id,
                   c.name AS category_name,
                   SUM(oi.quantity * oi.price_at_purchase) AS revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            JOIN product_variants pv ON oi.product_variant_id = pv.id
            JOIN products p ON pv.product_id = p.id
            JOIN categories c ON p.category_id = c.id
            WHERE o.created_at >= :from AND o.created_at < :to
              AND o.status != 'CANCELLED'::order_status
            GROUP BY c.id, c.name
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> findCategoryRevenueByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.created_at >= :from AND o.created_at < :to
              AND o.status != 'CANCELLED'::order_status
            """, nativeQuery = true)
    Long sumItemsSoldByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT COALESCE(SUM(oi.quantity * oi.price_at_purchase), 0)
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.created_at >= :from AND o.created_at < :to
              AND o.status != 'CANCELLED'::order_status
            """, nativeQuery = true)
    java.math.BigDecimal sumProductRevenueByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
