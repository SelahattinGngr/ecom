package selahattin.dev.ecom.repository.site;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.site.SiteSettingEntity;

@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSettingEntity, String> {

    Optional<SiteSettingEntity> findBySettingKey(String key);
}
