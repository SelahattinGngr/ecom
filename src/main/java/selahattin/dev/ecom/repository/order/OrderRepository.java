package selahattin.dev.ecom.repository.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {

    // Müşteri tarafı
    List<OrderEntity> findAllByUserId(UUID userId);

    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<OrderEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // --- ADMIN TARAFI ---

    // Tüm siparişleri getir (Tarihe göre sıralı)
    Page<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Statüye göre filtrele (Örn: Sadece PENDING olanlar)
    Page<OrderEntity> findAllByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    // --- PERFORMANCE: N+1 FIX ---

    @Query(value = "SELECT DISTINCT o FROM OrderEntity o JOIN FETCH o.user LEFT JOIN FETCH o.items",
           countQuery = "SELECT COUNT(o) FROM OrderEntity o")
    Page<OrderEntity> findAllWithDetails(Pageable pageable);

    @Query(value = "SELECT DISTINCT o FROM OrderEntity o JOIN FETCH o.user LEFT JOIN FETCH o.items WHERE o.status = :status",
           countQuery = "SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status")
    Page<OrderEntity> findAllWithDetailsByStatus(@Param("status") OrderStatus status, Pageable pageable);

    // --- EXPIRY ---

    @Query(value = "SELECT * FROM orders WHERE status = 'PENDING'::order_status AND created_at < :threshold",
           nativeQuery = true)
    List<OrderEntity> findExpiredPendingOrders(@Param("threshold") OffsetDateTime threshold);

    // --- ANALİTİK ---

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :from AND o.createdAt < :to")
    Long countByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.createdAt >= :from AND o.createdAt < :to")
    java.math.BigDecimal sumRevenueByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT o.status, COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :from AND o.createdAt < :to GROUP BY o.status")
    List<Object[]> countByStatusAndPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('day', created_at AT TIME ZONE :tz), 'YYYY-MM-DD') AS day,
                   COUNT(*) AS order_count,
                   COALESCE(SUM(total_amount), 0) AS revenue
            FROM orders
            WHERE created_at >= :from AND created_at < :to
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> dailyOrderStats(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
            @Param("tz") String tz);

    @Query("SELECT COUNT(DISTINCT o.user.id) FROM OrderEntity o WHERE o.createdAt >= :from AND o.createdAt < :to")
    Long countDistinctCustomersByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (shipped_at - created_at)) / 3600)
            FROM orders
            WHERE created_at >= :from AND created_at < :to
            AND shipped_at IS NOT NULL
            """, nativeQuery = true)
    Double averageShippingTimeHours(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT day_of_week, hour, orders
            FROM (
                SELECT TO_CHAR(DATE_TRUNC('day', created_at AT TIME ZONE :tz), 'Dy') AS day_of_week,
                       EXTRACT(HOUR FROM created_at AT TIME ZONE :tz)::int AS hour,
                       COUNT(*) AS orders
                FROM orders
                WHERE created_at >= :from AND created_at < :to
                GROUP BY 1, 2
            ) sub
            ORDER BY
                CASE day_of_week
                    WHEN 'Mon' THEN 1 WHEN 'Tue' THEN 2 WHEN 'Wed' THEN 3
                    WHEN 'Thu' THEN 4 WHEN 'Fri' THEN 5 WHEN 'Sat' THEN 6 WHEN 'Sun' THEN 7
                END, hour
            """, nativeQuery = true)
    List<Object[]> hourlyOrderHeatmap(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
            @Param("tz") String tz);
}