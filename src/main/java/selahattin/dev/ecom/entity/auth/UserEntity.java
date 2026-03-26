package selahattin.dev.ecom.entity.auth;

import java.time.OffsetDateTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import selahattin.dev.ecom.entity.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "phone_number_verified_at")
    private OffsetDateTime phoneNumberVerifiedAt;

    /**
     * LAZY: Roller ve izinler yalnızca fetch-join sorgusu veya açık transaction içinde yüklenir.
     * EAGER'dan LAZY'ye geçiş, her kullanıcı sorgusunda gereksiz join yapılmasını önler.
     * Roller gereken yerlerde UserRepository#findByIdFetchRoles / findByEmailAndDeletedAtIsNullFetchRoles kullanılır.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles;
}
