package selahattin.dev.ecom.service.domain;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.request.product.ProductFilterRequest;
import selahattin.dev.ecom.dto.response.product.CachedProductPage;
import selahattin.dev.ecom.dto.response.product.ImageResponse;
import selahattin.dev.ecom.dto.response.product.ProductResponse;
import selahattin.dev.ecom.dto.response.product.VariantResponse;
import selahattin.dev.ecom.entity.catalog.ProductEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.catalog.ProductRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String CACHE_SHOWCASE    = "prd:showcase";
    private static final String CACHE_BESTSELLERS = "prd:bestsellers";
    private static final String CACHE_SLUG_PREFIX = "prd:slug:";
    private static final String CACHE_LIST_PREFIX = "prd:list:";

    private static final long TTL_SHOWCASE_HOURS    = 24;
    private static final long TTL_BESTSELLERS_HOURS = 1;
    private static final long TTL_SLUG_MINUTES      = 10;
    private static final long TTL_LIST_MINUTES      = 5;

    private static final String VARIANT_ALIAS    = "variants";
    private static final String DELETED_AT_FIELD = "deletedAt";

    private final ProductRepository productRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------ //
    //  Public read methods                                                  //
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(ProductFilterRequest filter, Pageable pageable) {
        String cacheKey = buildListCacheKey(filter, pageable);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                CachedProductPage cp = objectMapper.readValue(cached, CachedProductPage.class);
                return new PageImpl<>(cp.getContent(), pageable, cp.getTotalElements());
            } catch (JsonProcessingException e) {
                log.warn("[CACHE] Ürün listesi deserialize hatası, DB'den yenileniyor.", e);
            }
        }

        Specification<ProductEntity> spec =
                Specification.where((root, q, cb) -> cb.isNull(root.get(DELETED_AT_FIELD)));

        if (StringUtils.hasText(filter.getQuery())) {
            String[] keywords = filter.getQuery().trim().split("\\s+");
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<ProductEntity, ProductVariantEntity> variants = root.join(VARIANT_ALIAS, JoinType.LEFT);
                List<Predicate> predicates = new ArrayList<>();
                for (String word : keywords) {
                    String pattern = "%" + word.toLowerCase(Locale.forLanguageTag("tr")) + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), pattern),
                            cb.like(cb.lower(root.get("description")), pattern),
                            cb.like(cb.lower(root.get("category").get("name")), pattern),
                            cb.like(cb.lower(variants.get("color")), pattern),
                            cb.like(cb.lower(variants.get("size")), pattern)));
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            });
        }

        if (filter.getCategoryId() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.equal(root.get("category").get("id"), filter.getCategoryId()));
        }

        if (filter.getMinPrice() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.greaterThanOrEqualTo(root.get("basePrice"), filter.getMinPrice()));
        }

        if (filter.getMaxPrice() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.lessThanOrEqualTo(root.get("basePrice"), filter.getMaxPrice()));
        }

        if (StringUtils.hasText(filter.getProductSize())) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<ProductEntity, ProductVariantEntity> variants = root.join(VARIANT_ALIAS);
                return cb.and(
                        cb.equal(variants.get("size"), filter.getProductSize()),
                        cb.isNull(variants.get(DELETED_AT_FIELD)),
                        cb.isTrue(variants.get("isActive")));
            });
        }

        if (StringUtils.hasText(filter.getColor())) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<ProductEntity, ProductVariantEntity> variants = root.join(VARIANT_ALIAS);
                return cb.and(
                        cb.equal(variants.get("color"), filter.getColor()),
                        cb.isNull(variants.get(DELETED_AT_FIELD)),
                        cb.isTrue(variants.get("isActive")));
            });
        }

        Page<ProductResponse> result = productRepository.findAll(spec, pageable)
                .map(entity -> mapToResponse(entity, false));

        try {
            CachedProductPage page = new CachedProductPage(result.getContent(), result.getTotalElements(),
                    pageable.getPageNumber(), pageable.getPageSize());
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(page),
                    Duration.ofMinutes(TTL_LIST_MINUTES));
        } catch (JsonProcessingException e) {
            log.error("[CACHE] Ürün listesi cache yazma hatası.", e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        String cacheKey = CACHE_SLUG_PREFIX + slug;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, ProductResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("[CACHE] Ürün slug deserialize hatası, DB'den yenileniyor.", e);
            }
        }

        ProductEntity product = productRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductResponse response = mapToResponse(product, true);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response),
                    Duration.ofMinutes(TTL_SLUG_MINUTES));
        } catch (JsonProcessingException e) {
            log.error("[CACHE] Ürün slug cache yazma hatası.", e);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getShowcaseProducts() {
        String cached = stringRedisTemplate.opsForValue().get(CACHE_SHOWCASE);
        if (cached != null) {
            try {
                CachedProductPage cp = objectMapper.readValue(cached, CachedProductPage.class);
                return cp.getContent();
            } catch (JsonProcessingException e) {
                log.warn("[CACHE] Vitrin ürünleri deserialize hatası, DB'den yenileniyor.", e);
            }
        }

        List<ProductResponse> result = productRepository.findShowcaseProducts().stream()
                .map(entity -> mapToResponse(entity, false))
                .toList();

        try {
            CachedProductPage page = new CachedProductPage(result, (long) result.size(), 0, result.size());
            stringRedisTemplate.opsForValue().set(CACHE_SHOWCASE, objectMapper.writeValueAsString(page),
                    Duration.ofHours(TTL_SHOWCASE_HOURS));
        } catch (JsonProcessingException e) {
            log.error("[CACHE] Vitrin ürünleri cache yazma hatası.", e);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getBestSellers() {
        String cached = stringRedisTemplate.opsForValue().get(CACHE_BESTSELLERS);
        if (cached != null) {
            try {
                CachedProductPage cp = objectMapper.readValue(cached, CachedProductPage.class);
                return cp.getContent();
            } catch (JsonProcessingException e) {
                log.warn("[CACHE] Çok satanlar deserialize hatası, DB'den yenileniyor.", e);
            }
        }

        List<ProductResponse> result = productRepository.findBestSellers().stream()
                .map(entity -> mapToResponse(entity, false))
                .toList();

        try {
            CachedProductPage page = new CachedProductPage(result, (long) result.size(), 0, result.size());
            stringRedisTemplate.opsForValue().set(CACHE_BESTSELLERS, objectMapper.writeValueAsString(page),
                    Duration.ofHours(TTL_BESTSELLERS_HOURS));
        } catch (JsonProcessingException e) {
            log.error("[CACHE] Çok satanlar cache yazma hatası.", e);
        }
        return result;
    }

    // ------------------------------------------------------------------ //
    //  Cache eviction — called by admin services                           //
    // ------------------------------------------------------------------ //

    public void evictSlugCache(String slug) {
        stringRedisTemplate.delete(CACHE_SLUG_PREFIX + slug);
    }

    public void evictShowcaseCache() {
        stringRedisTemplate.delete(CACHE_SHOWCASE);
    }

    public void evictProductListCache() {
        List<String> keys = new ArrayList<>();
        stringRedisTemplate.execute((RedisConnection conn) -> {
            ScanOptions opts = ScanOptions.scanOptions()
                    .match(CACHE_LIST_PREFIX + "*")
                    .count(100)
                    .build();
            try (Cursor<byte[]> cursor = conn.scan(opts)) {
                cursor.forEachRemaining(k -> keys.add(new String(k, StandardCharsets.UTF_8)));
            } catch (Exception e) {
                log.warn("[CACHE] prd:list:* tarama hatası: {}", e.getMessage());
            }
            return null;
        });
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                      //
    // ------------------------------------------------------------------ //

    private String buildListCacheKey(ProductFilterRequest filter, Pageable pageable) {
        String raw = String.join("|",
                Objects.toString(filter.getQuery(), ""),
                Objects.toString(filter.getCategoryId(), ""),
                Objects.toString(filter.getMinPrice(), ""),
                Objects.toString(filter.getMaxPrice(), ""),
                Objects.toString(filter.getProductSize(), ""),
                Objects.toString(filter.getColor(), ""),
                String.valueOf(pageable.getPageNumber()),
                String.valueOf(pageable.getPageSize()),
                pageable.getSort().toString());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return CACHE_LIST_PREFIX + sb;
        } catch (NoSuchAlgorithmException e) {
            return CACHE_LIST_PREFIX + Integer.toHexString(raw.hashCode());
        }
    }

    private ProductResponse mapToResponse(ProductEntity entity, boolean isDetailMode) {
        List<VariantResponse> variants = null;
        String description = null;
        String categoryName = null;
        Integer categoryId = null;
        String categorySlug = null;

        if (isDetailMode) {
            description = entity.getDescription();
            if (entity.getCategory() != null) {
                categoryName = entity.getCategory().getName();
                categoryId = entity.getCategory().getId();
                categorySlug = entity.getCategory().getSlug();
            }
            variants = (entity.getVariants() == null) ? Collections.emptyList()
                    : entity.getVariants().stream()
                            .filter(v -> v.getDeletedAt() == null && Boolean.TRUE.equals(v.getIsActive()))
                            .map(v -> VariantResponse.builder()
                                    .id(v.getId())
                                    .sku(v.getSku())
                                    .size(v.getSize())
                                    .color(v.getColor())
                                    .price(v.getPrice())
                                    .stockQuantity(v.getStockQuantity())
                                    .build())
                            .toList();
        }

        BigDecimal displayPrice = entity.getVariants() == null ? entity.getBasePrice()
                : entity.getVariants().stream()
                        .filter(v -> v.getDeletedAt() == null && Boolean.TRUE.equals(v.getIsActive())
                                && v.getPrice() != null)
                        .map(ProductVariantEntity::getPrice)
                        .min(BigDecimal::compareTo)
                        .orElse(entity.getBasePrice());

        List<ImageResponse> images = (entity.getImages() == null) ? Collections.emptyList()
                : entity.getImages().stream()
                        .filter(img -> img.getDeletedAt() == null)
                        .map(img -> ImageResponse.builder()
                                .id(img.getId())
                                .url(img.getUrl())
                                .displayOrder(img.getDisplayOrder())
                                .isThumbnail(img.getIsThumbnail())
                                .build())
                        .toList();

        return ProductResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .slug(entity.getSlug())
                .isShowcase(entity.getIsShowcase())
                .basePrice(displayPrice)
                .images(images)
                .description(description)
                .categoryName(categoryName)
                .categoryId(categoryId)
                .categorySlug(categorySlug)
                .variants(variants)
                .build();
    }
}
