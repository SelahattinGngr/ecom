package selahattin.dev.ecom.entity.site;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "site_asset_slots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteAssetSlotEntity {

    @Id
    @Column(name = "slot_key", nullable = false)
    private String slotKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private AssetEntity asset;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "title")
    private String title;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}