package selahattin.dev.ecom.repository.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.auth.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
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
}