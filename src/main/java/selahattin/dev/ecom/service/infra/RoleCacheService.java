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

    // Redis Prefix: security:roles:admin
    private static final String ROLE_CACHE_PREFIX = "security:roles:";

    /**
     * Tüm Rolleri ve İzinleri DB'den çekip Redis'e yükler.
     * Uygulama başladığında veya Admin panelinden rol güncellendiğinde çağrılır.
     */
    public void refreshRoleCache() {
        log.info("Rol ve Yetkiler önbelleğe yükleniyor...");
        List<RoleEntity> roles = roleRepository.findAll();

        for (RoleEntity role : roles) {
            String key = ROLE_CACHE_PREFIX + role.getName();

            // Permission isimlerini listeye çevir
            Set<String> permissions = role.getPermissions().stream()
                    .map(PermissionEntity::getName)
                    .collect(Collectors.toSet());

            // Redis'e kaydet (Süresiz veya çok uzun süreli)
            // permissions setini direkt saklıyoruz.
            redisTemplate.opsForValue().set(key, new ArrayList<>(permissions));
        }
        log.info("{} adet rol önbelleğe alındı.", roles.size());
    }

    /**
     * Verilen rol isminin izinlerini Redis'ten getirir.
     * Bulamazsa (Cache silinmişse) DB'den yükleyip tekrar cache'ler (Fail-safe).
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsForRole(String roleName) {
        String key = ROLE_CACHE_PREFIX + roleName;
        Object cachedPermissions = redisTemplate.opsForValue().get(key);

        if (cachedPermissions != null) {
            return (List<String>) cachedPermissions;
        }

        // Cache miss durumunda DB'ye git (Güvenlik önlemi)
        log.warn("Cache miss for role: {}. Fetching from DB...", roleName);
        RoleEntity role = roleRepository.findByName(roleName).orElse(null);

        if (role == null)
            return new ArrayList<>();

        List<String> permissions = role.getPermissions().stream()
                .map(PermissionEntity::getName)
                .toList();

        redisTemplate.opsForValue().set(key, permissions);
        return permissions;
    }
}