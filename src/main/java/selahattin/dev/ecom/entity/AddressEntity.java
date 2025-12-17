package selahattin.dev.ecom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "addresses")
public class AddressEntity extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "district_id", nullable = false)
    private DistrictEntity district;

    @Column(nullable = false, length = 100)
    private String neighborhood;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fullAddress;

    @Column(length = 20)
    private String zipCode;

    @Column(nullable = false, length = 100)
    private String contactName;

    @Column(nullable = false, length = 20)
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
}