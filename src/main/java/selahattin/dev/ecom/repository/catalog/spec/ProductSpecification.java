package selahattin.dev.ecom.repository.catalog.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import selahattin.dev.ecom.dto.request.product.AdminProductFilterRequest;
import selahattin.dev.ecom.entity.catalog.ProductEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.utils.enums.ProductStatus;

public class ProductSpecification {

    public static Specification<ProductEntity> withFilter(
        AdminProductFilterRequest filter
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            ProductStatus status =
                filter.getStatus() != null
                    ? filter.getStatus()
                    : ProductStatus.ACTIVE;
            if (status == ProductStatus.ACTIVE) {
                predicates.add(cb.isNull(root.get("deletedAt")));
            } else if (status == ProductStatus.DELETED) {
                predicates.add(cb.isNotNull(root.get("deletedAt")));
            }

            // Varyant join gerekip gerekmediğini belirle
            boolean needsVariantJoin =
                StringUtils.hasText(filter.getQuery()) ||
                StringUtils.hasText(filter.getProductSize()) ||
                StringUtils.hasText(filter.getColor()) ||
                StringUtils.hasText(filter.getSku()) ||
                filter.getStockMin() != null ||
                filter.getStockMax() != null ||
                filter.getIsVariantActive() != null;

            Join<ProductEntity, ProductVariantEntity> variants = null;
            if (needsVariantJoin) {
                query.distinct(true);
                variants = root.join("variants", JoinType.LEFT);
            }

            // 1. Kelime bazlı arama (İsim, Açıklama, Kategori, Renk, Beden)
            if (StringUtils.hasText(filter.getQuery())) {
                String[] keywords = filter.getQuery().trim().split("\\s+");
                List<Predicate> keywordPredicates = new ArrayList<>();
                for (String word : keywords) {
                    String pattern =
                        "%" +
                        word.toLowerCase(Locale.forLanguageTag("tr")) +
                        "%";
                    keywordPredicates.add(
                        cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern),
                            cb.like(
                                cb.lower(root.get("category").get("name")),
                                pattern
                            ),
                            cb.like(cb.lower(variants.get("color")), pattern),
                            cb.like(cb.lower(variants.get("size")), pattern)
                        )
                    );
                }
                predicates.add(
                    cb.and(keywordPredicates.toArray(new Predicate[0]))
                );
            }

            // 2. Kategori
            if (filter.getCategoryId() != null) {
                predicates.add(
                    cb.equal(
                        root.get("category").get("id"),
                        filter.getCategoryId()
                    )
                );
            }

            // 3. Min fiyat
            if (filter.getMinPrice() != null) {
                predicates.add(
                    cb.greaterThanOrEqualTo(
                        root.get("basePrice"),
                        filter.getMinPrice()
                    )
                );
            }

            // 4. Max fiyat
            if (filter.getMaxPrice() != null) {
                predicates.add(
                    cb.lessThanOrEqualTo(
                        root.get("basePrice"),
                        filter.getMaxPrice()
                    )
                );
            }

            // 5. Beden
            if (StringUtils.hasText(filter.getProductSize())) {
                predicates.add(
                    cb.equal(variants.get("size"), filter.getProductSize())
                );
            }

            // 6. Renk
            if (StringUtils.hasText(filter.getColor())) {
                predicates.add(
                    cb.equal(variants.get("color"), filter.getColor())
                );
            }

            // 7. SKU (admin-specific)
            if (StringUtils.hasText(filter.getSku())) {
                predicates.add(
                    cb.like(
                        cb.lower(variants.get("sku")),
                        "%" +
                            filter
                                .getSku()
                                .toLowerCase(Locale.forLanguageTag("tr")) +
                            "%"
                    )
                );
            }

            // 8. Min stok (admin-specific)
            if (filter.getStockMin() != null) {
                predicates.add(
                    cb.greaterThanOrEqualTo(
                        variants.get("stockQuantity"),
                        filter.getStockMin()
                    )
                );
            }

            // 9. Max stok (admin-specific)
            if (filter.getStockMax() != null) {
                predicates.add(
                    cb.lessThanOrEqualTo(
                        variants.get("stockQuantity"),
                        filter.getStockMax()
                    )
                );
            }

            // 10. Varyant aktiflik durumu (admin-specific)
            if (filter.getIsVariantActive() != null) {
                predicates.add(
                    cb.equal(
                        variants.get("isActive"),
                        filter.getIsVariantActive()
                    )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
