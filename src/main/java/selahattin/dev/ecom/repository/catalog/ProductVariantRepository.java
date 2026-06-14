package selahattin.dev.ecom.repository.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

    long countByProductId(UUID productId);

    Optional<ProductVariantEntity> findBySku(String sku);

    @Query(value = "SELECT v FROM ProductVariantEntity v " +
                   "JOIN FETCH v.product p " +
                   "LEFT JOIN FETCH p.category " +
                   "WHERE v.deletedAt IS NULL AND v.isActive = true AND p.deletedAt IS NULL",
           countQuery = "SELECT COUNT(v) FROM ProductVariantEntity v " +
                        "JOIN v.product p " +
                        "WHERE v.deletedAt IS NULL AND v.isActive = true AND p.deletedAt IS NULL")
    Page<ProductVariantEntity> findAllActiveWithProduct(Pageable pageable);

    @Query("SELECT v FROM ProductVariantEntity v " +
           "JOIN FETCH v.product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE v.deletedAt IS NULL AND v.isActive = true AND p.id = :productId AND p.deletedAt IS NULL " +
           "ORDER BY v.createdAt DESC")
    List<ProductVariantEntity> findByProductIdActiveWithProduct(@Param("productId") UUID productId);

    @Query("SELECT COUNT(v) FROM ProductVariantEntity v " +
           "WHERE v.product.id = :productId AND v.deletedAt IS NULL AND v.isActive = true")
    long countActiveByProductId(@Param("productId") UUID productId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE product_variants SET stock_quantity = stock_quantity - :qty " +
            "WHERE id = :id AND stock_quantity >= :qty AND deleted_at IS NULL", nativeQuery = true)
    int decreaseStock(@Param("id") UUID id, @Param("qty") int qty);

    /**
     * Stok iade — sipariş iptali veya başarısız ödeme sonrasında kullanılır.
     * SELECT + setField + save() döngüsünü (N+1) elimine eden direkt UPDATE.
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProductVariantEntity v SET v.stockQuantity = v.stockQuantity + :qty WHERE v.id = :id")
    void increaseStock(@Param("id") UUID id, @Param("qty") int qty);
}
