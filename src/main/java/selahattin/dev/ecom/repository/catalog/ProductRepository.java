package selahattin.dev.ecom.repository.catalog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.catalog.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {
    // Public taraf için (Silinmemişleri getir)
    Optional<ProductEntity> findBySlugAndDeletedAtIsNull(String slug);

    // Slug unique kontrolü için
    boolean existsBySlugAndDeletedAtIsNull(String slug);

    // Admin tarafı için (Silinmiş olsa bile slug ile bulabilmek için)
    Optional<ProductEntity> findBySlug(String slug);

    @Query(value = "SELECT COUNT(*) FROM products WHERE deleted_at IS NULL", nativeQuery = true)
    Long countTotalProducts();

    @Query(value = """
            SELECT COUNT(DISTINCT p.id)
            FROM products p
            JOIN product_variants pv ON pv.product_id = p.id
            WHERE p.deleted_at IS NULL AND pv.is_active = true AND pv.stock_quantity > 0
            """, nativeQuery = true)
    Long countActiveProducts();

    @Query(value = """
            SELECT COUNT(*) FROM products p
            WHERE p.deleted_at IS NULL
              AND NOT EXISTS (
                SELECT 1 FROM product_variants pv
                WHERE pv.product_id = p.id AND pv.is_active = true AND pv.stock_quantity > 0
              )
            """, nativeQuery = true)
    Long countOutOfStockProducts();

    @Query(value = """
            SELECT COUNT(*) FROM (
              SELECT p.id FROM products p
              JOIN product_variants pv ON pv.product_id = p.id AND pv.is_active = true
              WHERE p.deleted_at IS NULL
              GROUP BY p.id
              HAVING SUM(pv.stock_quantity) BETWEEN 1 AND 10
            ) sub
            """, nativeQuery = true)
    Long countLowStockProducts();
}
