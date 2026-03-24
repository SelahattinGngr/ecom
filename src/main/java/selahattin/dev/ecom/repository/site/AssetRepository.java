package selahattin.dev.ecom.repository.site;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.site.AssetEntity;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {
}
