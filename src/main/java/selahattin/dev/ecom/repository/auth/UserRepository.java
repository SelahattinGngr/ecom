package selahattin.dev.ecom.repository.auth;

import java.util.Optional;
import java.util.UUID;

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
}