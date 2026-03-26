package selahattin.dev.ecom.service.infra;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.entity.auth.PermissionEntity;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.repository.auth.RoleRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleCacheService {

    private final RoleRepository roleRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ROLE_CACHE_PREFIX = "security:roles:";

    /**
     * Tüm rolleri ve izinleri DB'den çekip Redis'e yükler.
     * Uygulama başladığında (CacheWarmupRunner) veya admin panelinden rol
     * güncellendiğinde çağrılır.
     *
     * findAllFetchPermissions() kullanımı: RoleEntity.permissions LAZY olduğundan
     * findAll() ile yüklenen roller üzerinde role.getPermissions() çağrısı
     * transaction bağlamı olmadığında LazyInitializationException fırlatır.
     * JOIN FETCH ile tek sorguda hem roller hem izinler yüklenir.
     */
    public void refreshRoleCache() {
        log.info("Rol ve yetkiler önbelleğe yükleniyor...");
        List<RoleEntity> roles = roleRepository.findAllFetchPermissions();

        for (RoleEntity role : roles) {
            String key = ROLE_CACHE_PREFIX + role.getName();

            Set<String> permissions = role.getPermissions().stream()
                    .map(PermissionEntity::getName)
                    .collect(Collectors.toSet());

            redisTemplate.opsForValue().set(key, new ArrayList<>(permissions));
        }
        log.info("{} adet rol önbelleğe alındı.", roles.size());
    }

    /**
     * Verilen rol isminin izinlerini Redis'ten getirir.
     * Cache miss durumunda DB'den yükleyip tekrar cache'ler (fail-safe).
     *
     * findByNameFetchPermissions() kullanımı: LAZY permissions alanının
     * transaction dışında okunabilmesi için JOIN FETCH sorgusu kullanılır.
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsForRole(String roleName) {
        String key = ROLE_CACHE_PREFIX + roleName;
        Object cachedPermissions = redisTemplate.opsForValue().get(key);

        if (cachedPermissions != null) {
            return (List<String>) cachedPermissions;
        }

        log.warn("Cache miss for role: {}. Fetching from DB...", roleName);
        RoleEntity role = roleRepository.findByNameFetchPermissions(roleName).orElse(null);

        if (role == null) {
            return new ArrayList<>();
        }

        List<String> permissions = role.getPermissions().stream()
                .map(PermissionEntity::getName)
                .toList();

        redisTemplate.opsForValue().set(key, permissions);
        return permissions;
    }
}
