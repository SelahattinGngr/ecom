package selahattin.dev.ecom.service.domain.admin;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.request.product.AdminProductFilterRequest;
import selahattin.dev.ecom.dto.request.product.CreateProductRequest;
import selahattin.dev.ecom.dto.request.product.ProductVariantRequest;
import selahattin.dev.ecom.dto.request.product.UpdateProductRequest;
import selahattin.dev.ecom.dto.response.product.ImageResponse;
import selahattin.dev.ecom.dto.response.product.ProductResponse;
import selahattin.dev.ecom.dto.response.product.VariantResponse;
import selahattin.dev.ecom.entity.catalog.CategoryEntity;
import selahattin.dev.ecom.entity.catalog.ProductEntity;
import selahattin.dev.ecom.entity.catalog.ProductImageEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;
import selahattin.dev.ecom.repository.catalog.ProductImageRepository;
import selahattin.dev.ecom.repository.catalog.ProductRepository;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.repository.catalog.spec.ProductSpecification;
import selahattin.dev.ecom.service.domain.ProductService;
import selahattin.dev.ecom.service.infra.FileStorageService;
import selahattin.dev.ecom.utils.SlugUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductsService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final EntityManager entityManager;
    private final ProductService productService;

    // --- PRODUCT CRUD ---

    @Transactional
    public ProductResponse createProductWithImages(CreateProductRequest request, List<MultipartFile> images) {
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        String slug = SlugUtils.toSlug(request.getName());
        if (productRepository.existsBySlugAndDeletedAtIsNull(slug)) {
            slug += "-" + UUID.randomUUID().toString().substring(0, 4);
        }

        ProductEntity product = ProductEntity.builder()
                .category(category)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .isShowcase(request.getIsShowcase() != null ? request.getIsShowcase() : false)
                .build();

        ProductEntity savedProduct = productRepository.save(product);

        if (images != null && !images.isEmpty()) {
            int order = 0;
            for (MultipartFile file : images) {
                String imageUrl = fileStorageService.save(file);
                ProductImageEntity image = ProductImageEntity.builder()
                        .product(savedProduct)
                        .url(imageUrl)
                        .displayOrder(order++)
                        .isThumbnail(order == 1)
                        .build();
                imageRepository.save(image);
            }
        }

        entityManager.flush();
        entityManager.refresh(savedProduct);

        productService.evictProductListCache();

        return mapToAdminResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        ProductEntity product = findProduct(id);

        boolean oldIsShowcase = Boolean.TRUE.equals(product.getIsShowcase());

        if (request.getName() != null)
            product.setName(request.getName());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getBasePrice() != null)
            product.setBasePrice(request.getBasePrice());
        if (request.getIsShowcase() != null)
            product.setIsShowcase(request.getIsShowcase());

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }

        String slug = product.getSlug();
        boolean newIsShowcase = Boolean.TRUE.equals(product.getIsShowcase());

        productRepository.save(product);

        productService.evictSlugCache(slug);
        productService.evictProductListCache();

        if (oldIsShowcase != newIsShowcase) {
            productService.evictShowcaseCache();
        }

        return mapToAdminResponse(product);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        ProductEntity product = findProduct(id);
        String slug = product.getSlug();
        String productName = product.getName();

        product.setDeletedAt(OffsetDateTime.now());
        productRepository.save(product);

        productService.evictSlugCache(slug);
        productService.evictProductListCache();

        try {
            auditLogService.log(
                    "PRODUCT_DELETED",
                    "PRODUCT",
                    id,
                    Map.of("productName", productName, "slug", slug));
        } catch (Exception logEx) {
            log.warn("[ADMIN] PRODUCT_DELETED audit log yazılamadı — productId: {}", id, logEx);
        }
    }

    // --- VARIANT MANAGEMENT ---

    @Transactional
    public void addVariant(UUID productId, ProductVariantRequest request) {
        ProductEntity product = findProduct(productId);

        ProductVariantEntity variant = ProductVariantEntity.builder()
                .product(product)
                .sku(request.getSku())
                .size(request.getProductSize())
                .color(request.getColor())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        variantRepository.save(variant);
    }

    @Transactional
    public void deleteVariant(UUID productId, UUID variantId) {
        ProductVariantEntity variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIANT_NOT_FOUND));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException(ErrorCode.VARIANT_MISMATCH);
        }

        variant.setDeletedAt(OffsetDateTime.now());
        variantRepository.save(variant);
    }

    // --- IMAGE MANAGEMENT ---

    @Transactional
    public void addImage(UUID productId, MultipartFile file, Integer displayOrder, Boolean isThumbnail) {
        ProductEntity product = findProduct(productId);
        String url = fileStorageService.save(file);

        ProductImageEntity image = ProductImageEntity.builder()
                .product(product)
                .url(url)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .isThumbnail(isThumbnail != null ? isThumbnail : false)
                .build();

        imageRepository.save(image);
    }

    @Transactional
    public void updateImage(UUID productId, UUID imageId, Integer displayOrder, Boolean isThumbnail) {
        ProductImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        if (!image.getProduct().getId().equals(productId)) {
            throw new BusinessException(ErrorCode.IMAGE_MISMATCH);
        }

        if (displayOrder != null)
            image.setDisplayOrder(displayOrder);
        if (isThumbnail != null) {
            if (isThumbnail) {
                imageRepository.clearThumbnailForProduct(productId, imageId);
            }
            image.setIsThumbnail(isThumbnail);
        }
        imageRepository.save(image);
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        ProductImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        fileStorageService.delete(image.getUrl());
        image.setDeletedAt(OffsetDateTime.now());
        imageRepository.save(image);
    }

    // --- PRODUCT GET ---

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(AdminProductFilterRequest filter, Pageable pageable) {
        return productRepository
                .findAll(ProductSpecification.withFilter(filter), pageable)
                .map(this::mapToAdminResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        ProductEntity product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return mapToAdminResponse(product);
    }

    // --- Helpers ---

    private ProductEntity findProduct(UUID id) {
        return productRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ProductResponse mapToAdminResponse(ProductEntity product) {
        List<VariantResponse> variantDtos = (product.getVariants() == null)
                ? Collections.emptyList()
                : product.getVariants().stream()
                        .filter(v -> v.getDeletedAt() == null)
                        .map(v -> VariantResponse.builder()
                                .id(v.getId())
                                .sku(v.getSku())
                                .size(v.getSize())
                                .color(v.getColor())
                                .price(v.getPrice())
                                .stockQuantity(v.getStockQuantity())
                                .isActive(v.getIsActive())
                                .createdAt(v.getCreatedAt())
                                .updatedAt(v.getUpdatedAt())
                                .deletedAt(v.getDeletedAt())
                                .build())
                        .toList();

        List<ImageResponse> imageDtos = (product.getImages() == null)
                ? Collections.emptyList()
                : product.getImages().stream()
                        .filter(i -> i.getDeletedAt() == null)
                        .map(i -> ImageResponse.builder()
                                .id(i.getId())
                                .url(i.getUrl())
                                .displayOrder(i.getDisplayOrder())
                                .isThumbnail(i.getIsThumbnail())
                                .build())
                        .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .isShowcase(product.getIsShowcase())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .categorySlug(product.getCategory().getSlug())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .deletedAt(product.getDeletedAt())
                .variants(variantDtos)
                .images(imageDtos)
                .build();
    }
}
