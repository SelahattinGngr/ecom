package selahattin.dev.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import selahattin.dev.ecom.entity.DistrictEntity;

public interface DistrictRepository extends JpaRepository<DistrictEntity, Integer> {
    List<DistrictEntity> findAllByCityIdOrderByNameAsc(Integer cityId);
}