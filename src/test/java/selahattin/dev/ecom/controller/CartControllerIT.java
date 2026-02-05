package selahattin.dev.ecom.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import selahattin.dev.ecom.AbstractIntegrationTest;
import selahattin.dev.ecom.dto.request.AddToCartRequest;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.catalog.CategoryEntity;
import selahattin.dev.ecom.entity.catalog.ProductEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;
import selahattin.dev.ecom.repository.catalog.ProductRepository;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.repository.order.CartItemRepository;
import selahattin.dev.ecom.repository.order.CartRepository;
import selahattin.dev.ecom.security.jwt.JwtTokenProvider;
import selahattin.dev.ecom.utils.enums.Role;

class CartControllerIT extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;
        @Autowired
        private UserRepository userRepository;
        @Autowired
        private RoleRepository roleRepository;
        @Autowired
        private ProductRepository productRepository;
        @Autowired
        private ProductVariantRepository productVariantRepository;
        @Autowired
        private CategoryRepository categoryRepository;
        @Autowired
        private CartRepository cartRepository;
        @Autowired
        private CartItemRepository cartItemRepository;
        @Autowired
        private RedisTemplate<String, Object> redisTemplate;
        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        private String userToken;
        private UUID variantId;
        private UserEntity testUser;

        @BeforeEach
        void setUp() {
                // 1. Create User
                RoleEntity role = roleRepository.findByName(Role.USER.name())
                                .orElseGet(() -> roleRepository
                                                .save(RoleEntity.builder().name(Role.USER.name()).build()));

                testUser = new UserEntity();
                testUser.setEmail("cart-user@test.com");
                testUser.setFirstName("Cart");
                testUser.setLastName("User");
                testUser.setRoles(new HashSet<>(List.of(role)));
                userRepository.save(testUser);

                userToken = jwtTokenProvider.generateAccessToken(testUser.getEmail(), testUser.getId().toString(),
                                testUser.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList()),
                                "device-1");

                // 2. Create Product & Variant
                CategoryEntity cat = categoryRepository.save(CategoryEntity.builder()
                                .name("Test Cat")
                                .slug("test-cat")
                                .build());

                ProductEntity product = ProductEntity.builder()
                                .category(cat)
                                .name("Cart Product")
                                .slug("cart-product")
                                .basePrice(BigDecimal.valueOf(100))
                                .build();
                productRepository.save(product);

                ProductVariantEntity variant = ProductVariantEntity.builder()
                                .product(product)
                                .sku("SKU-1")
                                .price(BigDecimal.valueOf(100))
                                .stockQuantity(10)
                                .isActive(true)
                                .build();
                productVariantRepository.save(variant);
                variantId = variant.getId();
        }

        @AfterEach
        void tearDown() {
                cartItemRepository.deleteAll();
                cartRepository.deleteAll();
                productVariantRepository.deleteAll();
                productRepository.deleteAll();
                userRepository.deleteAll();
                // Clear Redis
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        }

        @Test
        void addToCart_ShouldAddItem_WhenCartIsEmpty() throws Exception {
                AddToCartRequest request = new AddToCartRequest();
                request.setVariantId(variantId);
                request.setQuantity(2);

                mockMvc.perform(post("/api/v1/cart/items")
                                .cookie(new Cookie("accessToken", userToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                                .andExpect(jsonPath("$.data.totalPrice").value(200.0));

                // Redis check
                Object cache = redisTemplate.opsForValue().get("cart:" + testUser.getId());
                assertThat(cache).isNotNull();
        }

        @Test
        void addToCart_ShouldMergeItems_WhenItemAlreadyExists() throws Exception {
                // 1. Add first time
                AddToCartRequest request = new AddToCartRequest();
                request.setVariantId(variantId);
                request.setQuantity(1);

                mockMvc.perform(post("/api/v1/cart/items")
                                .cookie(new Cookie("accessToken", userToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // 2. Add second time
                mockMvc.perform(post("/api/v1/cart/items")
                                .cookie(new Cookie("accessToken", userToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
        }

        @Test
        void removeCartItem_ShouldRemoveItem() throws Exception {
                // 1. Add item first
                AddToCartRequest request = new AddToCartRequest();
                request.setVariantId(variantId);
                request.setQuantity(1);

                mockMvc.perform(post("/api/v1/cart/items")
                                .cookie(new Cookie("accessToken", userToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                // Find the item ID from DB
                UUID itemId = cartItemRepository.findAll().get(0).getId();

                // 2. Remove
                mockMvc.perform(delete("/api/v1/cart/items/" + itemId)
                                .cookie(new Cookie("accessToken", userToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.items").isEmpty());
        }
}
