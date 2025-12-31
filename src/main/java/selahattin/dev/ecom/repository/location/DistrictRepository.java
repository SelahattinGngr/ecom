package selahattin.dev.ecom.repository.location;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.location.DistrictEntity;

@Repository
public interface DistrictRepository extends JpaRepository<DistrictEntity, Integer> {
    List<DistrictEntity> findAllByCityIdOrderByNameAsc(Integer cityId);
}