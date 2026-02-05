package selahattin.dev.ecom.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import selahattin.dev.ecom.AbstractIntegrationTest;
import selahattin.dev.ecom.dto.request.product.CreateProductRequest;
import selahattin.dev.ecom.entity.auth.PermissionEntity;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.catalog.CategoryEntity;
import selahattin.dev.ecom.repository.auth.PermissionRepository;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;
import selahattin.dev.ecom.repository.catalog.ProductRepository;
import selahattin.dev.ecom.security.jwt.JwtTokenProvider;
import selahattin.dev.ecom.utils.enums.Role;

class AdminProductControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Integer categoryId;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // 1. Create Category
        if (categoryRepository.count() == 0) {
            CategoryEntity category = CategoryEntity.builder().name("Integration Config").slug("integration-config")
                    .build();
            CategoryEntity savedCategory = categoryRepository.save(category);
            categoryId = savedCategory.getId();
        } else {
            categoryId = categoryRepository.findAll().get(0).getId();
        }

        // 2. Setup Admin User with Permissions
        setupAdminUser();
    }

    private void setupAdminUser() {
        // Create permissions
        PermissionEntity createPerm = permissionRepository.findByName("product:create")
                .orElseGet(() -> permissionRepository
                        .save(PermissionEntity.builder().name("product:create").description("Create Product").build()));

        RoleEntity adminRole = roleRepository.findByName(Role.ADMIN.name()).orElseGet(() -> {
            RoleEntity r = new RoleEntity();
            r.setName(Role.ADMIN.name());
            roleRepository.save(r);
            return r;
        });

        // Add permission to role (if not present)
        if (!adminRole.getPermissions().contains(createPerm)) {
            adminRole.getPermissions().add(createPerm);
            roleRepository.save(adminRole);
        }

        UserEntity admin = new UserEntity();
        admin.setEmail("admin@test.com");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setRoles(new HashSet<>(List.of(adminRole)));
        userRepository.save(admin);

        // Use Mocking Strategy or Real Token Generation
        // Here we generate a real token for the integrated security filter
        adminToken = jwtTokenProvider.generateAccessToken(admin.getEmail(), admin.getId().toString(),
                admin.getRoles().stream().map(RoleEntity::getName).toList(), "test-device-id");
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
        // Do not delete categories or roles to avoid constraint issues across tests or
        // just clean them carefully
    }

    @Test
    void createProduct_ShouldSucceed_WhenUserHasPermission() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setCategoryId(categoryId);
        request.setName("New Product");
        request.setDescription("Super cool product");
        request.setBasePrice(BigDecimal.valueOf(100.00));

        MockMultipartFile dataInfo = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request));

        // Note: Missing 'images' part is optional in controller

        mockMvc.perform(multipart("/api/v1/admin/products")
                .file(dataInfo)
                .cookie(new Cookie("accessToken", adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Product"));

        assertThat(productRepository.existsBySlugAndDeletedAtIsNull("new-product")).isTrue();
    }

    @Test
    void createProduct_ShouldFail_WhenUserIsNotAuthorized() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setCategoryId(categoryId);
        request.setName("Unauthorized Product");
        request.setBasePrice(BigDecimal.TEN);

        MockMultipartFile dataInfo = new MockMultipartFile(
                "data",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/v1/admin/products")
                .file(dataInfo)) // No Token
                .andExpect(status().isUnauthorized());
    }
}
