package selahattin.dev.ecom.service.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.product.ProductFilterRequest;
import selahattin.dev.ecom.dto.response.product.ImageResponse;
import selahattin.dev.ecom.dto.response.product.ProductResponse;
import selahattin.dev.ecom.dto.response.product.VariantResponse;
import selahattin.dev.ecom.entity.catalog.ProductEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.catalog.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

	private static String variantAlias = "variants";
	private static String deletedAtField = "deletedAt";

	private final ProductRepository productRepository;

	/**
	 * CATALOG LISTELEME (LITE MODE)
	 * Sadece Resim, İsim, Fiyat döner. Description ve Varyantlar NULL döner.
	 */
	@Transactional(readOnly = true)
	public Page<ProductResponse> getProducts(ProductFilterRequest filter, Pageable pageable) {

		Specification<ProductEntity> spec = Specification.where((root, q, cb) -> cb.isNull(root.get(deletedAtField)));

		// 1. Gelişmiş Arama (Kelime Bazlı: İsim + Açıklama + Kategori + Varyant Rengi +
		// Varyant Özelliği)
		if (StringUtils.hasText(filter.getQuery())) {
			String[] keywords = filter.getQuery().trim().split("\\s+"); // Kelimeleri ayır

			spec = spec.and((root, query, cb) -> {
				query.distinct(true); // Duplicate önle

				// Varyant tablosuna sol taraftan bağlan (Join)
				Join<ProductEntity, ProductVariantEntity> variants = root.join(variantAlias, JoinType.LEFT);

				List<Predicate> predicates = new ArrayList<>();

				for (String word : keywords) {
					String pattern = "%" + word.toLowerCase(Locale.forLanguageTag("tr")) + "%";

					Predicate wordMatch = cb.or(
							cb.like(cb.lower(root.get("name")), pattern), // Ürün Adı
							cb.like(cb.lower(root.get("description")), pattern), // Açıklama
							cb.like(cb.lower(root.get("category").get("name")), pattern), // Kategori Adı

							// --- VARYANTLARDA ARA ---
							cb.like(cb.lower(variants.get("color")), pattern), // Renk (Gri)
							cb.like(cb.lower(variants.get("size")), pattern) // Beden/Özellik (32GB, XL)
					);
					predicates.add(wordMatch);
				}

				// Tüm kelimeler eşleşmeli (AND)
				return cb.and(predicates.toArray(new Predicate[0]));
			});
		}

		// 2. Kategori Filtresi
		if (filter.getCategoryId() != null) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("category").get("id"), filter.getCategoryId()));
		}

		// 3. Fiyat Filtresi (Min)
		if (filter.getMinPrice() != null) {
			spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("basePrice"), filter.getMinPrice()));
		}

		// 4. Fiyat Filtresi (Max)
		if (filter.getMaxPrice() != null) {
			spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("basePrice"), filter.getMaxPrice()));
		}

		// --- VARYANT BAZLI FİLTRELER (JOIN İŞLEMİ) ---

		// 5. Size (Beden/Özellik) Filtresi - (DTO'da ismini değiştirdiysen burayı
		// güncelle: filter.getVariantSize())
		if (StringUtils.hasText(filter.getSize())) {
			spec = spec.and((root, query, cb) -> {
				query.distinct(true);
				Join<ProductEntity, ProductVariantEntity> variants = root.join(variantAlias);
				return cb.and(
						cb.equal(variants.get("size"), filter.getSize()),
						cb.isNull(variants.get(deletedAtField)),
						cb.isTrue(variants.get("isActive")));
			});
		}

		// 6. Color (Renk) Filtresi
		if (StringUtils.hasText(filter.getColor())) {
			spec = spec.and((root, query, cb) -> {
				query.distinct(true);
				Join<ProductEntity, ProductVariantEntity> variants = root.join(variantAlias);
				return cb.and(
						cb.equal(variants.get("color"), filter.getColor()),
						cb.isNull(variants.get(deletedAtField)),
						cb.isTrue(variants.get("isActive")));
			});
		}

		// MAPPER'A 'FALSE' GÖNDERİYORUZ -> DETAY YOK
		return (Page<ProductResponse>) productRepository.findAll(spec, pageable)
				.map(entity -> mapToResponse(entity, false));
	}

	/**
	 * DETAY SAYFASI (FULL MODE)
	 * Her şeyi (Varyantlar, Stok, Açıklama, Kategori Detayı) döner.
	 */
	@Transactional(readOnly = true)
	public ProductResponse getProductBySlug(String slug) {
		ProductEntity product = productRepository.findBySlugAndDeletedAtIsNull(slug)
				.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

		// MAPPER'A 'TRUE' GÖNDERİYORUZ -> DETAY VAR
		return mapToResponse(product, true);
	}

	// --- Mapper ---
	private ProductResponse mapToResponse(ProductEntity entity, boolean isDetailMode) {

		List<VariantResponse> variants = null;
		String description = null;
		String categoryName = null;
		Integer categoryId = null;

		// SADECE DETAY İSTENİRSE BU ALANLARI DOLDUR
		if (isDetailMode) {
			description = entity.getDescription();

			// Kategori bilgisini detayda göster (Listede istenirse burayı if dışına
			// alabilirsin)
			if (entity.getCategory() != null) {
				categoryName = entity.getCategory().getName();
				categoryId = entity.getCategory().getId();
			}

			// Varyantları sadece detayda göster
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
				.basePrice(entity.getBasePrice()) // Listelemede Fiyat Lazım
				.images(images) // Listelemede Resim Lazım

				// Aşağıdakiler isDetailMode=false ise NULL döner (Trafik tasarrufu)
				.description(description)
				.categoryName(categoryName)
				.categoryId(categoryId)
				.variants(variants)
				.build();
	}
}