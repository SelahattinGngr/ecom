package selahattin.dev.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import selahattin.dev.ecom.entity.location.CityEntity;

public interface CityRepository extends JpaRepository<CityEntity, Integer> {
    List<CityEntity> findAllByOrderByNameAsc();

    List<CityEntity> findAllByCountryIdOrderByNameAsc(Integer countryId);
}