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
public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    // SADECE aktif kullanıcıyı bulur. Silinmişse yok sayar.
    Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

    // Kayıt olurken de sadece aktif kullanıcı var mı diye bakmalısın.
    // Silinmiş bir kullanıcının mailiyle yeni hesap açılmasına izin veriyoruz
    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    boolean existsByRoles_Id(UUID roleId);

    // Silinmemiş kullanıcıları sayfalayarak getir
    Page<UserEntity> findAllByDeletedAtIsNull(Pageable pageable);

    // ID ile silinmemiş kullanıcı getir (Zaten findById var ama deleted check için
    // custom yazılabilir veya serviste filter yapılabilir)
    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<UserEntity> findAllByRoles(RoleEntity role, Pageable pageable);

    // --- ANALİTİK ---

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.deletedAt IS NULL")
    Long countTotalActiveUsers();

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :from AND u.createdAt < :to AND u.deletedAt IS NULL")
    Long countNewUsersByPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('day', created_at AT TIME ZONE :tz), 'YYYY-MM-DD') AS day,
                   COUNT(*) AS new_users
            FROM users
            WHERE created_at >= :from AND created_at < :to
              AND deleted_at IS NULL
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> dailyRegistrationStats(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
            @Param("tz") String tz);
}
