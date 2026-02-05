package selahattin.dev.ecom.service.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import selahattin.dev.ecom.dto.request.product.ProductFilterRequest;
import selahattin.dev.ecom.dto.response.product.ProductResponse;
import selahattin.dev.ecom.entity.catalog.CategoryEntity;
import selahattin.dev.ecom.entity.catalog.ProductEntity;
import selahattin.dev.ecom.repository.catalog.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProducts_ShouldReturnListOfProducts_WhenFilterIsEmpty() {
        // Arrange
        ProductFilterRequest filter = new ProductFilterRequest();
        Pageable pageable = Pageable.unpaged();

        ProductEntity product = new ProductEntity();
        product.setId(java.util.UUID.randomUUID());
        product.setName("Test Product");
        // Create category separately
        CategoryEntity category = new CategoryEntity();
        category.setName("Electronics");
        category.setId(1);
        product.setCategory(category);

        Page<ProductEntity> productPage = new PageImpl<>(Collections.singletonList(product));

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(productPage);

        // Act
        Page<ProductResponse> result = productService.getProducts(filter, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Product");
        verify(productRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getProductBySlug_ShouldReturnProductDetails_WhenSlugExists() {
        // Arrange
        String slug = "test-product";
        ProductEntity product = new ProductEntity();
        product.setSlug(slug);
        product.setName("Detailed Product");
        product.setDescription("Description");

        // Create category separately
        CategoryEntity category = new CategoryEntity();
        category.setName("Detailed Cat");
        category.setId(10);
        product.setCategory(category);

        when(productRepository.findBySlugAndDeletedAtIsNull(slug)).thenReturn(Optional.of(product));

        // Act
        ProductResponse response = productService.getProductBySlug(slug);

        // Assert
        assertThat(response.getSlug()).isEqualTo(slug);
        assertThat(response.getDescription()).isEqualTo("Description"); // Detail mode = true
        verify(productRepository).findBySlugAndDeletedAtIsNull(slug);
    }
}
