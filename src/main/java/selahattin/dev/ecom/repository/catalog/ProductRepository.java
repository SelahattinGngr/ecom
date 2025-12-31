package selahattin.dev.ecom.repository.catalog;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.catalog.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {
    // Public taraf için (Silinmemişleri getir)
    Optional<ProductEntity> findBySlugAndDeletedAtIsNull(String slug);

    // Slug unique kontrolü için
    boolean existsBySlugAndDeletedAtIsNull(String slug);

    // Admin tarafı için (Silinmiş olsa bile ID ile bulabilmek için findById
    // yeterli)

}
