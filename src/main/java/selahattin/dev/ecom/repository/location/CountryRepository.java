package selahattin.dev.ecom.repository.location;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.location.CountryEntity;

@Repository
public interface CountryRepository extends JpaRepository<CountryEntity, Integer> {
    @Query("SELECT c.name FROM CountryEntity c ORDER BY c.name ASC")
    List<String> findAllCountries();
}
