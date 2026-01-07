package selahattin.dev.ecom.dev;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.entity.auth.PermissionEntity;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.catalog.CategoryEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.auth.PermissionRepository;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;
import selahattin.dev.ecom.repository.catalog.ProductImageRepository;
import selahattin.dev.ecom.repository.catalog.ProductRepository;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;

@Slf4j
@Order(1) // Diğer CommandLineRunner'lardan önce çalışsın
@RequiredArgsConstructor
public class CreateUserBean implements CommandLineRunner {
    // --- Auth ---
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    // --- Catalog ---
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public void run(String... args) throws Exception {
        createAdmin();
        createCustomers();
        roleCreate();
        createModerators();
        createCategories();
        createProducts();
        createProductVariants();
        createProductImages();
    }

    private void roleCreate() {
        String roleName = "product-manager";

        if (roleRepository.findByName(roleName).isPresent()) {
            log.info("Role zaten mevcut: " + roleName);
            return;
        }

        try {
            List<String> desiredPermissions = List.of(
                    "product:read",
                    "product:create",
                    "product:update",
                    "category:manage",
                    "dashboard:view");

            Set<PermissionEntity> permissions = new HashSet<>(
                    permissionRepository.findByNameIn(desiredPermissions));

            RoleEntity modRole = RoleEntity.builder()
                    .name(roleName)
                    .description("Moderator role with limited permissions (Product Focus)")
                    .isSystem(false)
                    .permissions(permissions)
                    .build();

            roleRepository.save(modRole);
            log.info("✅ Custom rol oluşturuldu: " + roleName);

        } catch (Exception e) {
            log.error("❌ Sahte rol oluşturulurken hata oluştu: " + e.getMessage());
        }
    }

    private void createAdmin() {
        String email = "selahattin_gungor53@hotmail.com";

        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            log.info("Developer kullanıcısı zaten mevcut, oluşturulmadı.");
            return;
        }

        try {
            RoleEntity developerRole = roleRepository.findByName("developer")
                    .orElseThrow(() -> new RuntimeException(
                            "HATA: 'developer' rolü veritabanında bulunamadı! init.sql çalıştı mı?"));

            OffsetDateTime now = OffsetDateTime.now();
            UserEntity adminUser = UserEntity.builder()
                    .firstName("Selahattin")
                    .lastName("Gungor")
                    .email(email)
                    .emailVerifiedAt(now)
                    .phoneNumber("+905418275359")
                    .phoneNumberVerifiedAt(now)
                    .roles(Set.of(developerRole))
                    .build();

            userRepository.save(adminUser);
            log.info("✅ Dev modunda Developer kullanıcısı başarıyla oluşturuldu.");

        } catch (Exception e) {
            log.error("❌ Sahte kullanıcı oluşturulurken hata: " + e.getMessage());
        }
    }

    private void createCustomers() {
        RoleEntity customerRole = roleRepository.findByName("customer")
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        for (int i = 1; i <= 1000; i++) {
            try {
                String email = "customer" + i + "@example.com";

                if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
                    continue;
                }

                OffsetDateTime now = OffsetDateTime.now();
                UserEntity customer = UserEntity.builder()
                        .firstName("Customer" + i)
                        .lastName("Test")
                        .email(email)
                        .emailVerifiedAt(now)
                        .phoneNumber("+9000000000" + i)
                        .phoneNumberVerifiedAt(now)
                        .roles(Set.of(customerRole))
                        .build();

                userRepository.save(customer);
                log.info("✅ Müşteri oluşturuldu: " + email);
            } catch (Exception e) {
                log.error("❌ Müşteri oluşturulurken hata: " + e.getMessage());
            }
        }
    }

    private void createModerators() {
        RoleEntity modRole = roleRepository.findByName("product-manager")
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        for (int i = 1; i <= 10; i++) {
            try {
                String email = "moderator" + i + "@example.com";

                if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
                    continue;
                }

                OffsetDateTime now = OffsetDateTime.now();
                UserEntity moderator = UserEntity.builder()
                        .firstName("Moderator" + i)
                        .lastName("Test")
                        .email(email)
                        .emailVerifiedAt(now)
                        .phoneNumber("+9011111111" + i)
                        .phoneNumberVerifiedAt(now)
                        .roles(Set.of(modRole))
                        .build();

                userRepository.save(moderator);
                log.info("✅ Moderator oluşturuldu: " + email);
            } catch (Exception e) {
                log.error("❌ Moderator oluşturulurken hata: " + e.getMessage());
            }
        }
    }

    private void createCategories() {
        for (int i = 1; i <= 5; i++) {
            try {
                String categoryName = "Category " + i;

                if (categoryRepository.existsByName(categoryName)) {
                    continue;
                }

                OffsetDateTime now = OffsetDateTime.now();
                CategoryEntity category = CategoryEntity.builder()
                        .name(categoryName)
                        .slug("category-" + i + "ABC")
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                categoryRepository.save(category);
                CategoryEntity category2 = CategoryEntity.builder()
                        .name(categoryName)
                        .slug("category-" + i + "DEF")
                        .parent(category)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                categoryRepository.save(category2);
                log.info("✅ Kategori oluşturuldu: " + categoryName);
            } catch (Exception e) {
                log.error("❌ Kategori oluşturulurken hata: " + e.getMessage());
            }
        }
    }

    private void createProducts() {
        // Ürünler oluşturma kodu buraya gelecek
    }

    private void createProductVariants() {
        // Ürün varyantları oluşturma kodu buraya gelecek
    }

    private void createProductImages() {
        // Ürün resimleri oluşturma kodu buraya gelecek
    }
}
