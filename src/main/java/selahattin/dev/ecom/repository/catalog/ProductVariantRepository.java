package selahattin.dev.ecom.repository.catalog;

import java.util.Optional;
import java.util.UUID;

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
