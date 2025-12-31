package selahattin.dev.ecom.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.service.infra.RoleCacheService;

@Order(2) // CreateUserBean (Order 1) çalıştıktan sonra çalışsın roller DB'de var olsun.
@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements CommandLineRunner {

    private final RoleCacheService roleCacheService;

    @Override
    public void run(String... args) throws Exception {
        roleCacheService.refreshRoleCache();
    }
}