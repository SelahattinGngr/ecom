package selahattin.dev.ecom.service.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.product.ImageResponse;
import selahattin.dev.ecom.dto.response.product.ProductResponse;
import selahattin.dev.ecom.dto.response.product.VariantResponse;
import selahattin.dev.ecom.entity.catalog.ProductEntity;
import selahattin.dev.ecom.exception.user.ResourceNotFoundException;
import selahattin.dev.ecom.repository.catalog.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String query, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        
        // Dinamik Filtreleme (Specification)
        Specification<ProductEntity> spec = Specification.where((root, q, cb) -> cb.isNull(root.get("deletedAt")));

        if (StringUtils.hasText(query)) {
            String likePattern = "%" + query.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), likePattern));
        }
        if (categoryId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (minPrice != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
        }

        return productRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        ProductEntity product = productRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + slug));
        return mapToResponse(product);
    }

    // --- Mapper ---
    private ProductResponse mapToResponse(ProductEntity entity) {
        // Varyantları Response'a çevir
        List<VariantResponse> variants = entity.getVariants().stream()
                .filter(v -> v.getDeletedAt() == null && v.getIsActive()) // Sadece aktif varyantlar
                .map(v -> VariantResponse.builder()
                        .id(v.getId())
                        .sku(v.getSku())
                        .size(v.getSize())
                        .color(v.getColor())
                        .price(v.getPrice())
                        .stockQuantity(v.getStockQuantity())
                        .build())
                .collect(Collectors.toList());

        // Görselleri Response'a çevir
        List<ImageResponse> images = entity.getImages().stream()
                .filter(img -> img.getDeletedAt() == null)
                .map(img -> ImageResponse.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .displayOrder(img.getDisplayOrder())
                        .isThumbnail(img.getIsThumbnail())
                        .build())
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .description(entity.getDescription())
                .basePrice(entity.getBasePrice())
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .variants(variants)
                .images(images)
                .build();
    }
}