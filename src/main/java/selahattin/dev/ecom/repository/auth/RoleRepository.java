package selahattin.dev.ecom.repository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.auth.RoleEntity;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByName(String name);

    boolean existsByName(String name);

    /**
     * Tüm rolleri izinleriyle birlikte tek sorguda getirir.
     * RoleEntity.permissions LAZY olduğundan, refreshRoleCache() gibi
     * transaction bağlamı dışındaki çağrılarda LazyInitializationException
     * yaşanmaması için bu sorgu kullanılır.
     */
    @Query("SELECT r FROM RoleEntity r LEFT JOIN FETCH r.permissions")
    List<RoleEntity> findAllFetchPermissions();

    /**
     * Tek bir rolü izinleriyle birlikte tek sorguda getirir.
     * Cache miss durumunda DB'den güvenle yüklemek için kullanılır.
     */
    @Query("SELECT r FROM RoleEntity r LEFT JOIN FETCH r.permissions WHERE r.name = :name")
    Optional<RoleEntity> findByNameFetchPermissions(@Param("name") String name);
}
