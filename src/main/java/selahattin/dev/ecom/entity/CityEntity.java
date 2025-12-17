package selahattin.dev.ecom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cities")
public class CityEntity {

    @Id
    @Column(nullable = false)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;
}