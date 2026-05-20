package selahattin.dev.ecom.service.domain;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        String cached = stringRedisTemplate.opsForValue().get(CACHE_CAT_TREE);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<CategoryResponse>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Kategori cache deserialize hatası, DB'den yenileniyor.", e);
            }
        }

        List<CategoryResponse> result = categoryRepository.findAllByParentIsNullAndDeletedAtIsNull().stream()
                .map(this::mapToResponse)
                .toList();

        try {
            stringRedisTemplate.opsForValue().set(CACHE_CAT_TREE, objectMapper.writeValueAsString(result),
                    Duration.ofHours(TTL_CAT_TREE_HOURS));
        } catch (JsonProcessingException e) {
            log.error("Kategori cache yazma hatası.", e);
        }

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
