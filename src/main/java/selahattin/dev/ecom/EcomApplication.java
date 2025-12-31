package selahattin.dev.ecom;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import selahattin.dev.ecom.dev.CreateUserBean;
import selahattin.dev.ecom.repository.auth.PermissionRepository;
import selahattin.dev.ecom.repository.auth.RoleRepository;
import selahattin.dev.ecom.repository.auth.UserRepository;
import selahattin.dev.ecom.repository.catalog.CategoryRepository;
import selahattin.dev.ecom.repository.catalog.ProductImageRepository;
import selahattin.dev.ecom.repository.catalog.ProductRepository;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;

@SpringBootApplication
public class EcomApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcomApplication.class, args);
	}

	@Bean
	@Profile("dev")
	public CommandLineRunner createUsersForDev(UserRepository userRepository, RoleRepository roleRepository,
			PermissionRepository permissionRepository, CategoryRepository categoryRepository,
			ProductImageRepository productImageRepository, ProductRepository productRepository,
			ProductVariantRepository productVariantRepository) {
		return new CreateUserBean(userRepository, roleRepository, permissionRepository, categoryRepository,
				productImageRepository, productRepository, productVariantRepository);
	}
}
