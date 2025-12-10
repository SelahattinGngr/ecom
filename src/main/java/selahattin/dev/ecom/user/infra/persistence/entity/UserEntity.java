package selahattin.dev.ecom.user.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import selahattin.dev.ecom.shared.jpa.BaseEntity;
import selahattin.dev.ecom.user.domain.enums.Role;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    private boolean isTwoFactorEnabled = false;

    @Builder.Default
    private boolean isActivated = false;

}