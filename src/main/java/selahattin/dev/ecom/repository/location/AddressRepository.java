package selahattin.dev.ecom.repository.location;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.location.AddressEntity;

@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {
    List<AddressEntity> findAllByUserId(UUID userId);
}