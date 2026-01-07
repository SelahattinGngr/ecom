package selahattin.dev.ecom.service.domain.admin;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.product.CreateImageRequest;
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
import selahattin.dev.ecom.service.infra.FileStorageService;
import selahattin.dev.ecom.utils.SlugUtils;

@Service
@RequiredArgsConstructor
public class AdminProductsService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final FileStorageService fileStorageService;

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
                .build();

        ProductEntity savedProduct = productRepository.save(product);

        if (images != null && !images.isEmpty()) {
            int order = 0;
            for (MultipartFile file : images) {
                String imageUrl = fileStorageService.save(file); // Dosyayı diske yaz, URL al

                ProductImageEntity image = ProductImageEntity.builder()
                        .product(savedProduct)
                        .url(imageUrl)
                        .displayOrder(order++)
                        .isThumbnail(order == 1) // İlk resim otomatik thumbnail olsun
                        .build();

                imageRepository.save(image);
            }
        }

        // FIXME (Varyantlar ve resimler boş dönebilir ilk başta, tekrar fetch
        // edebilirsin)
        return mapToAdminResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        ProductEntity product = findProduct(id);

        if (request.getName() != null)
            product.setName(request.getName());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getBasePrice() != null)
            product.setBasePrice(request.getBasePrice());

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }

        productRepository.save(product);
        return mapToAdminResponse(product);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        ProductEntity product = findProduct(id);
        product.setDeletedAt(OffsetDateTime.now()); // Soft Delete
        productRepository.save(product);

        // TODO İlişkili varyantlar da soft delete yapılabilir
    }

    // --- VARIANT MANAGEMENT ---

    @Transactional
    public void addVariant(UUID productId, ProductVariantRequest request) {
        ProductEntity product = findProduct(productId);

        // TODO SKU check yapılacak

        ProductVariantEntity variant = ProductVariantEntity.builder()
                .product(product)
                .sku(request.getSku())
                .size(request.getSize())
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
    public void addImage(UUID productId, CreateImageRequest request) {
        ProductEntity product = findProduct(productId);

        ProductImageEntity image = ProductImageEntity.builder()
                .product(product)
                .url(request.getUrl())
                .displayOrder(request.getDisplayOrder())
                .isThumbnail(request.getIsThumbnail())
                .build();

        imageRepository.save(image);
    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        ProductImageEntity image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        image.setDeletedAt(OffsetDateTime.now());
        imageRepository.save(image);
    }

    // Helper
    private ProductEntity findProduct(UUID id) {
        return productRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ProductResponse mapToAdminResponse(ProductEntity product) {

        // Varyant Listesini Hazırla
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
                                .build())
                        .toList();

        // Resim Listesini Hazırla
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
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .variants(variantDtos)
                .images(imageDtos)
                .build();
    }
}