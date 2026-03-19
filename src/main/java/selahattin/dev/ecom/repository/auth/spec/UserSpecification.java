package selahattin.dev.ecom.repository.auth.spec;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import selahattin.dev.ecom.entity.auth.RoleEntity;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.dto.request.admin.AdminUserFilterRequest;
import selahattin.dev.ecom.utils.enums.UserStatus;

public class UserSpecification {

    public static Specification<UserEntity> withFilter(AdminUserFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Arama Filtresi (İsim, Soyisim veya Email içinde case-insensitive arama)
            if (StringUtils.hasText(filter.getSearch())) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";

                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), searchPattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), searchPattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), searchPattern);

                predicates.add(cb.or(firstNameMatch, lastNameMatch, emailMatch));
            }

            // Status Filtresi
            if (filter.getStatus() != null) {
                if (filter.getStatus() == UserStatus.ACTIVE) {
                    predicates.add(cb.isNull(root.get("deletedAt")));
                } else if (filter.getStatus() == UserStatus.DELETED) {
                    predicates.add(cb.isNotNull(root.get("deletedAt")));
                }
                // ALL durumu için hiçbir WHERE şartı eklemiyoruz.
            } else {
                // Güvenlik amaçlı default fallback (status null gelirse aktifleri getir)
                predicates.add(cb.isNull(root.get("deletedAt")));
            }

            // Role Filtresi
            if (StringUtils.hasText(filter.getRoleName())) {
                Join<UserEntity, RoleEntity> rolesJoin = root.join("roles");
                predicates.add(cb.equal(rolesJoin.get("name"), filter.getRoleName()));

                // ManyToMany join'lerde duplicate kayıtları ve yanlış sayfalama count'unu
                // engellemek için distinct şarttır.
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}