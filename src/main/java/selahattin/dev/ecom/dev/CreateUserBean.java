package selahattin.dev.ecom.dev;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.entity.auth.PermissionEntity;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.catalog.CategoryEntity;
import selahattin.dev.ecom.entity.catalog.ProductEntity;
import selahattin.dev.ecom.entity.catalog.ProductImageEntity;
import selahattin.dev.ecom.entity.catalog.ProductVariantEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.auth.PermissionRepository;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;
import selahattin.dev.ecom.repository.catalog.ProductImageRepository;
import selahattin.dev.ecom.repository.catalog.ProductRepository;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.utils.SlugUtils;

@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class CreateUserBean implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🛠️ Dev ortamı veri yapılandırması başlatılıyor...");

        createAdmin();
        createCustomers();
        roleCreate();
        createModerators();
        createCategories();
        createProducts();
        createProductVariants();
        createProductImages();

        log.info("✅ Tüm sahte veriler başarıyla yüklendi.");
    }

    private void roleCreate() {
        String roleName = "product-manager";
        if (roleRepository.findByName(roleName).isPresent())
            return;

        try {
            List<String> desiredPermissions = List.of(
                    "product:read", "product:create", "product:update",
                    "category:manage", "dashboard:view");

            Set<PermissionEntity> permissions = new HashSet<>(
                    permissionRepository.findByNameIn(desiredPermissions));

            RoleEntity modRole = RoleEntity.builder()
                    .name(roleName)
                    .description("Moderator role (Product Focus)")
                    .isSystem(false)
                    .permissions(permissions)
                    .build();

            roleRepository.save(modRole);
        } catch (Exception e) {
            log.error("❌ Rol hatası: " + e.getMessage());
        }
    }

    private void createAdmin() {
        String email = "selahattin_gungor53@hotmail.com";
        if (userRepository.existsByEmailAndDeletedAtIsNull(email))
            return;

        roleRepository.findByName("developer").ifPresent(role -> {
            userRepository.save(UserEntity.builder()
                    .firstName("Selahattin").lastName("Gungor").email(email)
                    .emailVerifiedAt(OffsetDateTime.now())
                    .roles(Set.of(role)).build());
        });
    }

    private void createCustomers() {
        roleRepository.findByName("customer").ifPresent(role -> {
            for (int i = 1; i <= 3; i++) {
                String email = "customer" + i + "@example.com";
                if (userRepository.existsByEmailAndDeletedAtIsNull(email))
                    continue;
                userRepository.save(UserEntity.builder()
                        .firstName("Customer" + i).lastName("Test").email(email)
                        .roles(Set.of(role)).build());
            }
        });
    }

    private void createModerators() {
        roleRepository.findByName("product-manager").ifPresent(role -> {
            String email = "mod@example.com";
            if (userRepository.existsByEmailAndDeletedAtIsNull(email))
                return;
            userRepository.save(UserEntity.builder()
                    .firstName("Mod").lastName("Test").email(email)
                    .roles(Set.of(role)).build());
        });
    }

    private void createCategories() {
        if (categoryRepository.count() > 0)
            return;

        CategoryEntity main = categoryRepository.save(CategoryEntity.builder()
                .name("Elektronik").slug("elektronik").build());

        categoryRepository.save(CategoryEntity.builder()
                .name("Telefon").slug("telefon").parent(main).build());
    }

    private void createProducts() {
        if (productRepository.count() > 0)
            return;
        List<CategoryEntity> categories = categoryRepository.findAll();

        for (int i = 1; i <= 5; i++) {
            String name = "Ürün " + i;
            productRepository.save(ProductEntity.builder()
                    .name(name)
                    .slug(SlugUtils.toSlug(name) + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .description("Açıklama " + i)
                    .basePrice(new BigDecimal("1000.00"))
                    .category(categories.get(0))
                    .build());
        }
    }

    private void createProductVariants() {
        List<ProductEntity> products = productRepository.findAll();

        for (ProductEntity product : products) {
            // Unique SKU üretimi için Product ID ve random string ekliyoruz
            String skuPrefix = product.getSlug().length() > 5 ? product.getSlug().substring(0, 5).toUpperCase()
                    : "PROD";
            String generatedSku = skuPrefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // Sadece bu ürüne ait varyant yoksa oluştur
            if (productVariantRepository.countByProductId(product.getId()) == 0) {
                productVariantRepository.save(ProductVariantEntity.builder()
                        .product(product)
                        .sku(generatedSku)
                        .size("Standart")
                        .color("Siyah")
                        .price(product.getBasePrice())
                        .stockQuantity(100)
                        .isActive(true)
                        .build());
            }
        }
    }

    private void createProductImages() {
        List<ProductEntity> products = productRepository.findAll();

        for (ProductEntity product : products) {
            if (productImageRepository.countByProductId(product.getId()) == 0) {
                productImageRepository.save(ProductImageEntity.builder()
                        .product(product)
                        .url("https://picsum.photos/200")
                        .displayOrder(1)
                        .isThumbnail(true)
                        .build());
            }
        }
    }
}