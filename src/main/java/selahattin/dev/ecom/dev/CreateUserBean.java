package selahattin.dev.ecom.dev;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
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
import selahattin.dev.ecom.entity.location.AddressEntity;
import selahattin.dev.ecom.entity.location.CityEntity;
import selahattin.dev.ecom.entity.location.CountryEntity;
import selahattin.dev.ecom.entity.location.DistrictEntity;
import selahattin.dev.ecom.entity.order.CartEntity;
import selahattin.dev.ecom.entity.order.CartItemEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.order.OrderItemEntity;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.entity.payment.RefundEntity;
import selahattin.dev.ecom.repository.auth.PermissionRepository;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;
import selahattin.dev.ecom.repository.catalog.ProductImageRepository;
import selahattin.dev.ecom.repository.catalog.ProductRepository;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.repository.location.AddressRepository;
import selahattin.dev.ecom.repository.location.CityRepository;
import selahattin.dev.ecom.repository.location.CountryRepository;
import selahattin.dev.ecom.repository.location.DistrictRepository;
import selahattin.dev.ecom.repository.order.CartItemRepository;
import selahattin.dev.ecom.repository.order.CartRepository;
import selahattin.dev.ecom.repository.order.OrderItemRepository;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.repository.payment.PaymentRepository;
import selahattin.dev.ecom.repository.payment.RefundRepository;
import selahattin.dev.ecom.utils.SlugUtils;
import selahattin.dev.ecom.utils.enums.OrderStatus;
import selahattin.dev.ecom.utils.enums.PaymentProvider;
import selahattin.dev.ecom.utils.enums.PaymentStatus;
import selahattin.dev.ecom.utils.enums.RefundStatus;

@Slf4j
@Order(1)
@Component
@Profile({ "dev", "test" })
@RequiredArgsConstructor
public class CreateUserBean implements CommandLineRunner {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PermissionRepository permissionRepository;
        private final CategoryRepository categoryRepository;
        private final ProductRepository productRepository;
        private final ProductVariantRepository productVariantRepository;
        private final ProductImageRepository productImageRepository;
        private final AddressRepository addressRepository;
        private final CountryRepository countryRepository;
        private final CityRepository cityRepository;
        private final DistrictRepository districtRepository;
        private final CartRepository cartRepository;
        private final CartItemRepository cartItemRepository;
        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;
        private final PaymentRepository paymentRepository;
        private final RefundRepository refundRepository;

        @Override
        @Transactional
        public void run(String... args) {
                log.info("🛠️  Dev seed başlatılıyor...");

                createRoles();
                createUsers();
                createCategories();
                createProducts();
                createAddresses();
                createCarts();
                createOrders();

                log.info("✅ Dev seed tamamlandı.");
                log.info("   {} kullanıcı, {} rol, {} kategori, {} ürün, {} sipariş oluşturuldu",
                                userRepository.count(), roleRepository.count(), categoryRepository.count(),
                                productRepository.count(), orderRepository.count());
        }

        // ─── ROLLER ────────────────────────────────────────────────────────────────

        private void createRoles() {
                if (roleRepository.findByName("product-manager").isPresent())
                        return;

                List<String> perms = List.of(
                                "product:read", "product:create", "product:update",
                                "category:manage", "dashboard:view");

                Set<PermissionEntity> permissions = new HashSet<>(permissionRepository.findByNameIn(perms));

                roleRepository.save(RoleEntity.builder()
                                .name("product-manager")
                                .description("Ürün yöneticisi — katalog işlemleri")
                                .isSystem(false)
                                .permissions(permissions)
                                .build());

                log.info("  ✓ Rol oluşturuldu: product-manager");
        }

        // ─── KULLANICILAR ──────────────────────────────────────────────────────────

        private void createUsers() {
                // Developer (tüm yetkiler)
                saveUser("selahattin_gungor53@hotmail.com", "Selahattin", "Gungor", "developer", true);
                // Admin
                saveUser("admin@example.com", "Admin", "User", "admin", true);
                // Müşteriler
                saveUser("customer1@example.com", "Ali", "Yılmaz", "customer", false);
                saveUser("customer2@example.com", "Ayşe", "Kaya", "customer", false);
                saveUser("customer3@example.com", "Mehmet", "Demir", "customer", false);
                saveUser("customer4@example.com", "Fatma", "Şahin", "customer", false);
                saveUser("customer5@example.com", "Hasan", "Çelik", "customer", false);
                saveUser("customer6@example.com", "Zeynep", "Arslan", "customer", false);
                saveUser("customer7@example.com", "Emre", "Koç", "customer", false);
                saveUser("customer8@example.com", "Selin", "Aydın", "customer", false);
                saveUser("customer9@example.com", "Burak", "Yıldız", "customer", false);
                saveUser("customer10@example.com", "Merve", "Özkan", "customer", false);
                // Ürün yöneticisi
                saveUser("mod@example.com", "Moderator", "Test", "product-manager", true);
        }

        private void saveUser(String email, String first, String last, String roleName, boolean verified) {
                if (userRepository.existsByEmailAndDeletedAtIsNull(email))
                        return;

                roleRepository.findByName(roleName).ifPresent(role -> {
                        UserEntity user = UserEntity.builder()
                                        .email(email)
                                        .firstName(first)
                                        .lastName(last)
                                        .emailVerifiedAt(verified ? OffsetDateTime.now() : null)
                                        .roles(Set.of(role))
                                        .build();
                        userRepository.save(user);
                        log.info("  ✓ Kullanıcı oluşturuldu: {} ({})", email, roleName);
                });
        }

        // ─── KATEGORİLER ───────────────────────────────────────────────────────────

        private void createCategories() {
                if (categoryRepository.existsBySlug("elektronik"))
                        return;

                CategoryEntity elektronik = save(cat("Elektronik", "elektronik", null));
                save(cat("Telefon & Aksesuar", "telefon-aksesuar", elektronik));
                save(cat("Bilgisayar & Tablet", "bilgisayar-tablet", elektronik));

                CategoryEntity giyim = save(cat("Giyim & Moda", "giyim-moda", null));
                save(cat("Erkek Giyim", "erkek-giyim", giyim));
                save(cat("Kadın Giyim", "kadin-giyim", giyim));

                save(cat("Spor & Outdoor", "spor-outdoor", null));

                log.info("  ✓ 7 kategori oluşturuldu");
        }

        private CategoryEntity cat(String name, String slug, CategoryEntity parent) {
                return CategoryEntity.builder().name(name).slug(slug).parent(parent).build();
        }

        private CategoryEntity save(CategoryEntity c) {
                return categoryRepository.save(c);
        }

        // ─── ÜRÜNLER ───────────────────────────────────────────────────────────────

        private void createProducts() {
                // Seed ürünlerimiz zaten varsa (SKU ile kontrol) geç
                if (productVariantRepository.findBySku("IP15PRO-SYH-256").isPresent())
                        return;

                List<CategoryEntity> cats = categoryRepository.findAll();
                CategoryEntity telefon = cats.stream().filter(c -> c.getSlug().equals("telefon-aksesuar")).findFirst()
                                .orElse(cats.get(0));
                CategoryEntity bilgisayar = cats.stream().filter(c -> c.getSlug().equals("bilgisayar-tablet"))
                                .findFirst().orElse(cats.get(0));
                CategoryEntity erkek = cats.stream().filter(c -> c.getSlug().equals("erkek-giyim")).findFirst()
                                .orElse(cats.get(0));
                CategoryEntity spor = cats.stream().filter(c -> c.getSlug().equals("spor-outdoor")).findFirst()
                                .orElse(cats.get(0));

                // iPhone 15 Pro — 2 varyant
                ProductEntity iphone = createProduct("iPhone 15 Pro", telefon, new BigDecimal("54999.99"),
                                "Apple iPhone 15 Pro 256GB. A17 Pro çip, titanyum tasarım, 48MP Pro kamera sistemi.");
                createVariant(iphone, "IP15PRO-SYH-256", "Titanium Siyah", "256GB", new BigDecimal("54999.99"), 30);
                createVariant(iphone, "IP15PRO-BYZ-512", "Beyaz Titanyum", "512GB", new BigDecimal("64999.99"), 15);
                createImage(iphone, "https://picsum.photos/seed/iphone/800/800");

                // Samsung Galaxy S24
                ProductEntity samsung = createProduct("Samsung Galaxy S24 Ultra", telefon, new BigDecimal("39999.99"),
                                "Samsung Galaxy S24 Ultra 256GB. Snapdragon 8 Gen 3, S-Pen dahil, 200MP kamera.");
                createVariant(samsung, "S24U-BLK-256", "Phantom Black", "256GB", new BigDecimal("39999.99"), 25);
                createVariant(samsung, "S24U-GRY-512", "Titanium Gray", "512GB", new BigDecimal("47999.99"), 10);
                createImage(samsung, "https://picsum.photos/seed/samsung/800/800");

                // MacBook Pro
                ProductEntity macbook = createProduct("MacBook Pro 14\"", bilgisayar, new BigDecimal("79999.99"),
                                "Apple MacBook Pro 14 inç M3 Pro çipli, 18GB RAM, 512GB SSD.");
                createVariant(macbook, "MBP14-SGR-512", "Space Gray", "512GB", new BigDecimal("79999.99"), 20);
                createVariant(macbook, "MBP14-SLV-1TB", "Gümüş", "1TB", new BigDecimal("94999.99"), 8);
                createImage(macbook, "https://picsum.photos/seed/macbook/800/800");

                // Erkek Tişört
                ProductEntity tisort = createProduct("Erkek Oversize Tişört", erkek, new BigDecimal("299.99"),
                                "Yüksek kalite pamuklu oversize fit erkek tişörtü. Nefes alabilir, rahat kesim.");
                createVariant(tisort, "TSHRT-SYH-S", "Siyah", "S", new BigDecimal("299.99"), 100);
                createVariant(tisort, "TSHRT-SYH-M", "Siyah", "M", new BigDecimal("299.99"), 150);
                createVariant(tisort, "TSHRT-SYH-L", "Siyah", "L", new BigDecimal("299.99"), 120);
                createVariant(tisort, "TSHRT-BYZ-M", "Beyaz", "M", new BigDecimal("299.99"), 80);
                createImage(tisort, "https://picsum.photos/seed/tisort/800/800");

                // Koşu Ayakkabısı
                ProductEntity ayakkabi = createProduct("Pro Runner X5 Koşu Ayakkabısı", spor, new BigDecimal("1299.99"),
                                "Hafif ve konforlu koşu ayakkabısı. React köpük taban, nefes alabilen mesh üst.");
                createVariant(ayakkabi, "RUNX5-SYH-42", "Siyah/Kırmızı", "42", new BigDecimal("1299.99"), 40);
                createVariant(ayakkabi, "RUNX5-SYH-43", "Siyah/Kırmızı", "43", new BigDecimal("1299.99"), 45);
                createVariant(ayakkabi, "RUNX5-MAV-44", "Mavi/Beyaz", "44", new BigDecimal("1299.99"), 35);
                createImage(ayakkabi, "https://picsum.photos/seed/ayakkabi/800/800");

                log.info("  ✓ 5 ürün, toplam {} varyant oluşturuldu",
                                productVariantRepository.count());
        }

        private ProductEntity createProduct(String name, CategoryEntity cat, BigDecimal price, String desc) {
                String slug = SlugUtils.toSlug(name) + "-" + UUID.randomUUID().toString().substring(0, 6);
                return productRepository.save(ProductEntity.builder()
                                .name(name).slug(slug).description(desc).basePrice(price).category(cat)
                                .build());
        }

        private void createVariant(ProductEntity product, String sku, String color, String size,
                        BigDecimal price, int stock) {
                productVariantRepository.save(ProductVariantEntity.builder()
                                .product(product).sku(sku).color(color).size(size)
                                .price(price).stockQuantity(stock).isActive(true)
                                .build());
        }

        private void createImage(ProductEntity product, String url) {
                productImageRepository.save(ProductImageEntity.builder()
                                .product(product).url(url).displayOrder(1).isThumbnail(true)
                                .build());
        }

        // ─── ADRESLER ──────────────────────────────────────────────────────────────

        private void createAddresses() {
                CountryEntity turkey = countryRepository.findById(1).orElse(null);
                if (turkey == null)
                        return;

                List<CityEntity> cities = cityRepository.findAllByCountryIdOrderByNameAsc(1);
                if (cities.isEmpty())
                        return;
                CityEntity istanbul = cities.stream()
                                .filter(c -> c.getName().toLowerCase().contains("istanbul")
                                                || c.getName().toLowerCase().contains("İstanbul"))
                                .findFirst()
                                .orElse(cities.get(0));

                List<DistrictEntity> districts = districtRepository.findAllByCityIdOrderByNameAsc(istanbul.getId());
                DistrictEntity district = districts.isEmpty() ? null : districts.get(0);

                saveAddress("customer1@example.com", "Ev", turkey, istanbul, district, "Ali Yılmaz", "+905551112233",
                                "Kadıköy Mah.", "Bağdat Cad. No:42 Daire:8", "34710");
                saveAddress("customer1@example.com", "İş", turkey, istanbul, district, "Ali Yılmaz", "+905551112233",
                                "Levent Mah.", "Büyükdere Cad. No:100 Kat:5", "34394");
                saveAddress("customer2@example.com", "Ev", turkey, istanbul, district, "Ayşe Kaya", "+905552223344",
                                "Beşiktaş Mah.", "Barbaros Bulvarı No:15 D:3", "34353");
                saveAddress("customer3@example.com", "Ev", turkey, istanbul, district, "Mehmet Demir", "+905553334455",
                                "Üsküdar Mah.", "Çamlıca Cad. No:7 Daire:2", "34674");
        }

        private void saveAddress(String email, String title, CountryEntity country, CityEntity city,
                        DistrictEntity district, String contactName, String contactPhone,
                        String neighborhood, String fullAddress, String postalCode) {
                userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
                        boolean exists = !addressRepository.findAllByUserId(user.getId()).isEmpty();
                        // customer1'in 2 adresi olacak — title ile ayırt et
                        boolean titleExists = addressRepository.findAllByUserId(user.getId()).stream()
                                        .anyMatch(a -> title.equals(a.getTitle()));
                        if (titleExists)
                                return;

                        addressRepository.save(AddressEntity.builder()
                                        .user(user).title(title).country(country).city(city).district(district)
                                        .contactName(contactName).contactPhone(contactPhone)
                                        .neighborhood(neighborhood).fullAddress(fullAddress).postalCode(postalCode)
                                        .build());
                });
        }

        // ─── SEPETLER ──────────────────────────────────────────────────────────────

        private void createCarts() {
                ProductVariantEntity v1 = findVariant("IP15PRO-SYH-256");
                ProductVariantEntity v2 = findVariant("S24U-BLK-256");
                ProductVariantEntity v3 = findVariant("TSHRT-SYH-M");

                if (v1 != null)
                        addToCart("customer1@example.com", v1, 1);
                if (v2 != null)
                        addToCart("customer1@example.com", v2, 2);
                if (v3 != null)
                        addToCart("customer3@example.com", v3, 1);
        }

        private void addToCart(String email, ProductVariantEntity variant, int qty) {
                userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
                        CartEntity cart = cartRepository.findByUserId(user.getId())
                                        .orElseGet(() -> cartRepository.save(CartEntity.builder().user(user).build()));

                        boolean alreadyInCart = cart.getItems() != null && cart.getItems().stream()
                                        .anyMatch(i -> i.getProductVariant().getId().equals(variant.getId()));
                        if (alreadyInCart)
                                return;

                        cartItemRepository.save(CartItemEntity.builder()
                                        .cart(cart).productVariant(variant).quantity(qty)
                                        .build());
                });
        }

        // ─── SİPARİŞLER ────────────────────────────────────────────────────────────

        private void createOrders() {
                if (orderRepository.count() > 0)
                        return;

                ProductVariantEntity iphoneV = findVariant("IP15PRO-SYH-256");
                ProductVariantEntity iphoneV2 = findVariant("IP15PRO-BYZ-512");
                ProductVariantEntity samsungV = findVariant("S24U-BLK-256");
                ProductVariantEntity samsungV2 = findVariant("S24U-GRY-512");
                ProductVariantEntity macbookV = findVariant("MBP14-SGR-512");
                ProductVariantEntity tisortM = findVariant("TSHRT-SYH-M");
                ProductVariantEntity ayakkabiV = findVariant("RUNX5-SYH-43");

                // Herhangi bir varyant eksikse sipariş oluşturma
                if (iphoneV == null || samsungV == null || macbookV == null
                                || tisortM == null || ayakkabiV == null) {
                        log.warn("  ⚠ Varyantlar bulunamadı, siparişler oluşturulamadı. " +
                                        "Önce ürünlerin seed edildiğinden emin ol.");
                        return;
                }

                UserEntity c1 = findUser("customer1@example.com");
                UserEntity c2 = findUser("customer2@example.com");
                UserEntity c3 = findUser("customer3@example.com");

                CountryEntity turkey = countryRepository.findById(1).orElse(null);
                CityEntity istanbul = cityRepository.findAllByCountryIdOrderByNameAsc(1).stream()
                                .filter(c -> c.getName().toLowerCase().contains("istanbul")
                                                || c.getName().toLowerCase().contains("İstanbul"))
                                .findFirst()
                                .orElseGet(() -> cityRepository.findAllByCountryIdOrderByNameAsc(1).get(0));
                List<DistrictEntity> dists = districtRepository.findAllByCityIdOrderByNameAsc(istanbul.getId());
                DistrictEntity district = dists.isEmpty() ? null : dists.get(0);

                Map<String, Object> addr1 = shippingAddr("Ali Yılmaz", "Bağdat Cad. No:42 D:8", istanbul.getName(),
                                "34710");
                Map<String, Object> addr2 = shippingAddr("Ayşe Kaya", "Barbaros Bulvarı No:15", istanbul.getName(),
                                "34353");
                Map<String, Object> addr3 = shippingAddr("Mehmet Demir", "Çamlıca Cad. No:7 D:2", istanbul.getName(),
                                "34674");

                if (c1 != null && turkey != null) {
                        // 1. PENDING — ödeme bekleniyor
                        OrderEntity pending = saveOrder(c1, OrderStatus.PENDING,
                                        iphoneV.getPrice(), addr1, turkey, istanbul, district,
                                        "Ali", "Yılmaz", "+905551112233");
                        addItem(pending, iphoneV, 1);
                        savePayment(pending, PaymentStatus.PENDING, "MOCK-PENDING-001", iphoneV.getPrice());
                        log.info("  ✓ Sipariş PENDING: {}", pending.getId());

                        // 2. PREPARING — ödeme alındı, hazırlanıyor
                        BigDecimal samsungTotal = samsungV.getPrice();
                        OrderEntity preparing = saveOrder(c1, OrderStatus.PREPARING,
                                        samsungTotal, addr1, turkey, istanbul, district,
                                        "Ali", "Yılmaz", "+905551112233");
                        addItem(preparing, samsungV, 1);
                        savePayment(preparing, PaymentStatus.SUCCEEDED, "MOCK-SUCC-002", samsungTotal);
                        log.info("  ✓ Sipariş PREPARING: {}", preparing.getId());

                        // 3. SHIPPED — kargoda
                        BigDecimal macTotal = macbookV.getPrice();
                        OrderEntity shipped = saveOrder(c1, OrderStatus.SHIPPED,
                                        macTotal, addr1, turkey, istanbul, district,
                                        "Ali", "Yılmaz", "+905551112233");
                        addItem(shipped, macbookV, 1);
                        savePayment(shipped, PaymentStatus.SUCCEEDED, "MOCK-SUCC-003", macTotal);
                        shipped.setCargoFirm("Yurtiçi Kargo");
                        shipped.setTrackingCode("YK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                        shipped.setShippedAt(OffsetDateTime.now().minusDays(2));
                        orderRepository.save(shipped);
                        log.info("  ✓ Sipariş SHIPPED: {}", shipped.getId());

                        // 4. DELIVERED — teslim edildi
                        BigDecimal tisortTotal = tisortM.getPrice().multiply(BigDecimal.valueOf(3));
                        OrderEntity delivered = saveOrder(c1, OrderStatus.DELIVERED,
                                        tisortTotal, addr1, turkey, istanbul, district,
                                        "Ali", "Yılmaz", "+905551112233");
                        addItem(delivered, tisortM, 3);
                        savePayment(delivered, PaymentStatus.SUCCEEDED, "MOCK-SUCC-004", tisortTotal);
                        log.info("  ✓ Sipariş DELIVERED: {}", delivered.getId());

                        // 5. CANCELLED — iptal edildi
                        ProductVariantEntity cancelVariant = iphoneV2 != null ? iphoneV2 : iphoneV;
                        OrderEntity cancelled = saveOrder(c1, OrderStatus.CANCELLED,
                                        cancelVariant.getPrice(), addr1, turkey, istanbul, district,
                                        "Ali", "Yılmaz", "+905551112233");
                        addItem(cancelled, cancelVariant, 1);
                        savePayment(cancelled, PaymentStatus.CANCELLED, "MOCK-CNCL-005", cancelVariant.getPrice());
                        log.info("  ✓ Sipariş CANCELLED: {}", cancelled.getId());
                }

                if (c2 != null && turkey != null) {
                        // 6. RETURN_REQUESTED — iade talebi bekliyor (admin test için)
                        BigDecimal ayakTotal = ayakkabiV.getPrice().multiply(BigDecimal.valueOf(2));
                        OrderEntity returnReq = saveOrder(c2, OrderStatus.RETURN_REQUESTED,
                                        ayakTotal, addr2, turkey, istanbul, district,
                                        "Ayşe", "Kaya", "+905552223344");
                        addItem(returnReq, ayakkabiV, 2);
                        PaymentEntity returnReqPayment = savePayment(returnReq, PaymentStatus.SUCCEEDED,
                                        "MOCK-SUCC-006", ayakTotal);
                        returnReq.setReturnReason("Ürün bedeni uymadı, değiştirmek istiyorum.");
                        returnReq.setReturnCode("RET-" + returnReq.getId().toString().substring(0, 8).toUpperCase());
                        returnReq.setReturnedAt(OffsetDateTime.now().minusDays(1));
                        orderRepository.save(returnReq);
                        log.info("  ✓ Sipariş RETURN_REQUESTED: {} (admin onay/red için)", returnReq.getId());

                        // 7. RETURNED — iade onaylandı ve ödeme iade edildi
                        ProductVariantEntity retVariant = samsungV2 != null ? samsungV2 : samsungV;
                        BigDecimal samsungV2Total = retVariant.getPrice();
                        OrderEntity returned = saveOrder(c2, OrderStatus.RETURNED,
                                        samsungV2Total, addr2, turkey, istanbul, district,
                                        "Ayşe", "Kaya", "+905552223344");
                        addItem(returned, retVariant, 1);
                        PaymentEntity refundedPayment = savePayment(returned, PaymentStatus.REFUNDED,
                                        "MOCK-RFND-007", samsungV2Total);
                        returned.setReturnReason("Ürün açıklamadaki özelliklerle uyuşmuyordu.");
                        returned.setReturnCode("RET-" + returned.getId().toString().substring(0, 8).toUpperCase());
                        returned.setReturnedAt(OffsetDateTime.now().minusDays(3));
                        orderRepository.save(returned);

                        refundRepository.save(RefundEntity.builder()
                                        .payment(refundedPayment)
                                        .providerRefundId("MOCK-REF-"
                                                        + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                        .amount(samsungV2Total)
                                        .status(RefundStatus.SUCCEEDED)
                                        .reason("Ürün açıklamadaki özelliklerle uyuşmuyordu.")
                                        .build());
                        log.info("  ✓ Sipariş RETURNED (iade tamamlandı): {}", returned.getId());
                }

                if (c3 != null && turkey != null) {
                        // 8. PAID — ödeme alındı henüz işleme alınmadı
                        BigDecimal paidTotal = iphoneV.getPrice();
                        OrderEntity paid = saveOrder(c3, OrderStatus.PAID,
                                        paidTotal, addr3, turkey, istanbul, district,
                                        "Mehmet", "Demir", "+905553334455");
                        addItem(paid, iphoneV, 1);
                        savePayment(paid, PaymentStatus.SUCCEEDED, "MOCK-SUCC-008", paidTotal);
                        log.info("  ✓ Sipariş PAID: {}", paid.getId());
                }

                log.info("  ✓ {} sipariş oluşturuldu", orderRepository.count());
        }

        // ─── YARDIMCI METODLAR ─────────────────────────────────────────────────────

        private ProductVariantEntity findVariant(String sku) {
                return productVariantRepository.findBySku(sku).orElse(null);
        }

        private UserEntity findUser(String email) {
                return userRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);
        }

        private Map<String, Object> shippingAddr(String contactName, String fullAddress,
                        String cityName, String postalCode) {
                return Map.of(
                                "contactName", contactName,
                                "fullAddress", fullAddress,
                                "cityName", cityName,
                                "postalCode", postalCode,
                                "countryName", "Türkiye");
        }

        private OrderEntity saveOrder(UserEntity user, OrderStatus status,
                        BigDecimal total, Map<String, Object> shippingAddr,
                        CountryEntity country, CityEntity city, DistrictEntity district,
                        String firstName, String lastName, String phone) {
                return orderRepository.save(OrderEntity.builder()
                                .user(user)
                                .status(status)
                                .totalAmount(total)
                                .shippingAddress(shippingAddr)
                                .billingAddress(shippingAddr)
                                .shippingCountry(country)
                                .shippingCity(city)
                                .shippingDistrict(district)
                                .shippingRecipientFirstName(firstName)
                                .shippingRecipientLastName(lastName)
                                .shippingRecipientPhoneNumber(phone)
                                .build());
        }

        private void addItem(OrderEntity order, ProductVariantEntity variant, int qty) {
                orderItemRepository.save(OrderItemEntity.builder()
                                .order(order)
                                .productVariant(variant)
                                .quantity(qty)
                                .priceAtPurchase(variant.getPrice())
                                .skuAtPurchase(variant.getSku())
                                .productNameAtPurchase(variant.getProduct().getName())
                                .variantSnapshot(Map.of(
                                                "color", variant.getColor() != null ? variant.getColor() : "",
                                                "size", variant.getSize() != null ? variant.getSize() : ""))
                                .build());
        }

        private PaymentEntity savePayment(OrderEntity order, PaymentStatus status,
                        String txId, BigDecimal amount) {
                return paymentRepository.save(PaymentEntity.builder()
                                .order(order)
                                .paymentProvider(PaymentProvider.IYZICO)
                                .paymentTransactionId(txId)
                                .amount(amount)
                                .status(status)
                                .description("Dev seed - " + status.name())
                                .build());
        }
}
