package selahattin.dev.ecom.service.domain;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.response.catalog.CategoryResponse;
import selahattin.dev.ecom.entity.catalog.CategoryEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String CACHE_CAT_TREE = "cat:tree";
    private static final long TTL_CAT_TREE_HOURS = 24;

    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<CategoryResponse> getAllCategories() {
        Object cached = redisTemplate.opsForValue().get(CACHE_CAT_TREE);
        if (cached instanceof List<?> list) {
            return (List<CategoryResponse>) list;
        }

        List<CategoryResponse> result = categoryRepository.findAllByParentIsNullAndDeletedAtIsNull().stream()
                .map(this::mapToResponse)
                .toList();

        redisTemplate.opsForValue().set(CACHE_CAT_TREE, result, Duration.ofHours(TTL_CAT_TREE_HOURS));
        return result;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        CategoryEntity category = categoryRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return mapToResponse(category);
    }

    private CategoryResponse mapToResponse(CategoryEntity entity) {
        if (entity == null)
            return null;

        List<CategoryResponse> subCategories = null;
        if (entity.getSubCategories() != null) {
            subCategories = entity.getSubCategories().stream()
                    .filter(sub -> sub.getDeletedAt() == null)
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
