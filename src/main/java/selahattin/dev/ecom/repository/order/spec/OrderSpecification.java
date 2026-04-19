package selahattin.dev.ecom.repository.order.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import selahattin.dev.ecom.dto.request.admin.AdminOrderFilterRequest;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;

public class OrderSpecification {

    public static Specification<OrderEntity> withFilter(AdminOrderFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Sipariş durumu
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // 2. Müşteri adı veya e-posta araması
            if (StringUtils.hasText(filter.getQuery())) {
                query.distinct(true);
                Join<OrderEntity, UserEntity> user = root.join("user", JoinType.LEFT);
                String pattern = "%" + filter.getQuery().trim().toLowerCase(Locale.forLanguageTag("tr")) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(user.get("firstName")), pattern),
                        cb.like(cb.lower(user.get("lastName")), pattern),
                        cb.like(cb.lower(user.get("email")), pattern)
                ));
            }

            // 3. Min tutar
            if (filter.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), filter.getMinAmount()));
            }

            // 4. Max tutar
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), filter.getMaxAmount()));
            }

            // 5. Başlangıç tarihi
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
            }

            // 6. Bitiş tarihi
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
            }

            // 7. Kargo firması
            if (StringUtils.hasText(filter.getCargoFirm())) {
                predicates.add(cb.like(
                        cb.lower(root.get("cargoFirm")),
                        "%" + filter.getCargoFirm().toLowerCase(Locale.forLanguageTag("tr")) + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
