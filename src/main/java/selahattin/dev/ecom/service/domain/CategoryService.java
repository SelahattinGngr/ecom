package selahattin.dev.ecom.service.domain;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.catalog.CategoryResponse;
import selahattin.dev.ecom.entity.catalog.CategoryEntity;
import selahattin.dev.ecom.exception.user.ResourceNotFoundException;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;

	@Transactional(readOnly = true)
	public List<CategoryResponse> getAllCategories() {
		// Sadece en üst (Parent'ı olmayan) kategorileri çekiyoruz.
		// Altındakileri Hibernate "Lazy Load" ile mapToResponse içinde çekecek.
		List<CategoryEntity> rootCategories = categoryRepository.findAllByParentIsNullAndDeletedAtIsNull();

		return rootCategories.stream()
				.map(this::mapToResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse getCategoryById(Integer id) {
		CategoryEntity category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı."));

		// Eğer silinmişse 404 ver (Soft delete kontrolü)
		if (category.getDeletedAt() != null) {
			throw new ResourceNotFoundException("Kategori bulunamadı.");
		}

		return mapToResponse(category);
	}

	// --- HELPER: RECURSIVE MAPPER ---

	private CategoryResponse mapToResponse(CategoryEntity entity) {
		if (entity == null)
			return null;

		// Alt kategorileri dönüştür (Kendini çağırıyor!)
		// deletedAt kontrolünü burada da yapıyoruz ki silinmiş alt kategoriler
		// gelmesin.
		List<CategoryResponse> subCategories = null;
		if (entity.getSubCategories() != null) {
			subCategories = entity.getSubCategories().stream()
					.filter(sub -> sub.getDeletedAt() == null) // Silinenleri ele
					.map(this::mapToResponse)
					.toList();
		}

		return CategoryResponse.builder()
				.id(entity.getId())
				.name(entity.getName())
				.slug(entity.getSlug())
				.imageUrl(entity.getImageUrl())
				.parentId(entity.getParent() != null ? entity.getParent().getId() : null)
				.subCategories(subCategories != null ? subCategories : Collections.emptyList())
				.build();
	}
}