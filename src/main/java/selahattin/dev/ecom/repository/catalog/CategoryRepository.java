package selahattin.dev.ecom.repository.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import selahattin.dev.ecom.entity.catalog.CategoryEntity;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer> {

    List<CategoryEntity> findAllByParentIsNullAndDeletedAtIsNull();

    public boolean existsByName(String name);
}
