package selahattin.dev.ecom.service.domain.admin;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.category.CreateCategoryRequest;
import selahattin.dev.ecom.dto.request.category.UpdateCategoryRequest;
import selahattin.dev.ecom.dto.response.catalog.CategoryResponse;
import selahattin.dev.ecom.entity.catalog.CategoryEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;
import selahattin.dev.ecom.utils.SlugUtils;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        // Sadece ANA (Root) kategorileri getir.
        // Alt kategoriler mapToResponse içinde recursive dolacak.
        return categoryRepository.findAllByParentIsNullAndDeletedAtIsNull().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String slug = SlugUtils.toSlug(request.getName());

        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        CategoryEntity parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        CategoryEntity category = CategoryEntity.builder()
                .name(request.getName())
                .slug(slug)
                .parent(parent)
                // imageUrl eklenebilir
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Integer id, UpdateCategoryRequest request) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (StringUtils.hasText(request.getName())) {
            category.setName(request.getName());

            if (StringUtils.hasText(request.getSlug())) {
                category.setSlug(request.getSlug());
            } else {
                category.setSlug(SlugUtils.toSlug(request.getName()));
            }
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(category.getId())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST,
                        "Kategori kendi kendisinin üst kategorisi olamaz.");
            }
            CategoryEntity parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            category.setParent(parent);
        }

        category.setUpdatedAt(OffsetDateTime.now());
        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Integer id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // Eğer alt kategorileri varsa silinmesini engelleyebiliriz
        category.setDeletedAt(OffsetDateTime.now());
        categoryRepository.save(category);
    }

    private CategoryResponse mapToResponse(CategoryEntity entity) {
        if (entity == null)
            return null;

        // Alt kategorileri recursive olarak dönüştür
        List<CategoryResponse> subCategories = null;
        if (entity.getSubCategories() != null) {
            subCategories = entity.getSubCategories().stream()
                    .filter(sub -> sub.getDeletedAt() == null) // Silinenleri gösterme
                    .map(this::mapToResponse) // Kendini çağır (Recursion)
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