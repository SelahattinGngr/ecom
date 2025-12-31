package selahattin.dev.ecom.repository.catalog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

}
