package selahattin.dev.ecom.repository.order;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.order.CartEntity;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, UUID> {

}
