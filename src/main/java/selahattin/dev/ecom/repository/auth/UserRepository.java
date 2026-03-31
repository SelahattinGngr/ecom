package selahattin.dev.ecom.repository.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;

@Repository
public interface UserRepository
    extends
        JpaRepository<UserEntity, UUID>,
        JpaSpecificationExecutor<UserEntity>
{
    // SADECE aktif kullanıcıyı bulur. Silinmişse yok sayar.
    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

    // Kayıt olurken de sadece aktif kullanıcı var mı diye bakmalısın.
    // Silinmiş bir kullanıcının mailiyle yeni hesap açılmasına izin veriyoruz
    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    boolean existsByRoles_Id(UUID roleId);

    // Silinmemiş kullanıcıları sayfalayarak getir
    Page<UserEntity> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<UserEntity> findAllByRoles(RoleEntity role, Pageable pageable);

    /**
     * Auth akışı için: e-posta ile kullanıcıyı rol ve izinleriyle birlikte tek sorguda yükler.
     * UserEntity.roles ve RoleEntity.permissions LAZY olduğundan, session oluşturma ve
     * profil görüntüleme gibi rollere ihtiyaç duyulan yerlerde bu sorgu kullanılmalıdır.
     */
    @Query(
        "SELECT u FROM UserEntity u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.email = :email AND u.deletedAt IS NULL"
    )
    Optional<UserEntity> findByEmailAndDeletedAtIsNullFetchRoles(
        @Param("email") String email
    );

    /**
     * Auth akışı için: ID ile kullanıcıyı rol ve izinleriyle birlikte tek sorguda yükler.
     * getCurrentUser() çağrılarında N+1'i önlemek için kullanılır.
     */
    @Query(
        "SELECT u FROM UserEntity u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.id = :id"
    )
    Optional<UserEntity> findByIdFetchRoles(@Param("id") UUID id);

    // --- ANALİTİK ---

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.deletedAt IS NULL")
    Long countTotalActiveUsers();

    @Query(
        "SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :from AND u.createdAt < :to AND u.deletedAt IS NULL"
    )
    Long countNewUsersByPeriod(
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );

    @Query(
        value = """
        SELECT TO_CHAR(DATE_TRUNC('day', created_at AT TIME ZONE :tz), 'YYYY-MM-DD') AS day,
               COUNT(*) AS new_users
        FROM users
        WHERE created_at >= :from AND created_at < :to
          AND deleted_at IS NULL
        GROUP BY 1
        ORDER BY 1
        """,
        nativeQuery = true
    )
    List<Object[]> dailyRegistrationStats(
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to,
        @Param("tz") String tz
    );

    @Query(
        "SELECT r.name, COUNT(u) FROM UserEntity u " +
            "JOIN u.roles r " +
            "WHERE u.deletedAt IS NULL " +
            "GROUP BY r.name"
    )
    List<Object[]> countUsersByRole();
}
