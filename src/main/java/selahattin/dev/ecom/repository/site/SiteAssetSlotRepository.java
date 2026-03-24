package selahattin.dev.ecom.repository.site;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.site.SiteAssetSlotEntity;

@Repository
public interface SiteAssetSlotRepository extends JpaRepository<SiteAssetSlotEntity, String> {

    Optional<SiteAssetSlotEntity> findBySlotKey(String slotKey);
}
