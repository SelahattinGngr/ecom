package selahattin.dev.ecom.repository.catalog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {
    long countByProductId(UUID productId);

    Optional<ProductVariantEntity> findBySku(String sku);

    @Modifying
    @Query("UPDATE ProductVariantEntity v SET v.stockQuantity = v.stockQuantity - :qty " +
            "WHERE v.id = :id AND v.stockQuantity >= :qty")
    int decreaseStock(UUID id, int qty);
}
