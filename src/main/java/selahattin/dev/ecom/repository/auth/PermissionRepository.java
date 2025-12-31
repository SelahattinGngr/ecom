package selahattin.dev.ecom.repository.auth;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.auth.PermissionEntity;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    Set<PermissionEntity> findByNameIn(Collection<String> names);
}
