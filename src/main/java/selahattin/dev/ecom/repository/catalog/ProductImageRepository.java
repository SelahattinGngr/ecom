package selahattin.dev.ecom.repository.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.catalog.ProductImageEntity;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImageEntity, UUID> {
    long countByProductId(UUID productId);

    @Modifying
    @Query("UPDATE ProductImageEntity i SET i.isThumbnail = false WHERE i.product.id = :productId AND i.isThumbnail = true AND i.id <> :excludeImageId")
    void clearThumbnailForProduct(UUID productId, UUID excludeImageId);
}
