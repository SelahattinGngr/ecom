package selahattin.dev.ecom.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import selahattin.dev.ecom.entity.auth.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    java.util.Optional<RoleEntity> findByName(String name);

}
