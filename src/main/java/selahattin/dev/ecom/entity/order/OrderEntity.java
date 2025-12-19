package selahattin.dev.ecom.entity.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.location.CityEntity;
import selahattin.dev.ecom.entity.location.CountryEntity;
import selahattin.dev.ecom.entity.location.DistrictEntity;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    // Shipping Recipient Info
    @Column(name = "shipping_recipient_first_name")
    private String shippingRecipientFirstName;

    @Column(name = "shipping_recipient_last_name")
    private String shippingRecipientLastName;

    @Column(name = "shipping_recipient_phone_number")
    private String shippingRecipientPhoneNumber;

    // JSONB Shipping Address
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> shippingAddress; // Veya özel bir DTO class

    // Location References
    @ManyToOne
    @JoinColumn(name = "shipping_country_id")
    private CountryEntity shippingCountry;

    @ManyToOne
    @JoinColumn(name = "shipping_city_id")
    private CityEntity shippingCity;

    @ManyToOne
    @JoinColumn(name = "shipping_district_id")
    private DistrictEntity shippingDistrict;

    @Column(name = "shipping_postal_code")
    private String shippingPostalCode;

    // JSONB Billing Address
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address", columnDefinition = "jsonb")
    private Map<String, Object> billingAddress;

    // Return Info
    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    @Column(name = "return_reason")
    private String returnReason;

    @Column(name = "return_tracking_no")
    private String returnTrackingNo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItemEntity> items;
}