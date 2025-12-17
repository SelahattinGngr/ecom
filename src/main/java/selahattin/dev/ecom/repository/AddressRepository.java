package selahattin.dev.ecom.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import selahattin.dev.ecom.entity.AddressEntity;

public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {
    List<AddressEntity> findAllByUserId(UUID userId);
}