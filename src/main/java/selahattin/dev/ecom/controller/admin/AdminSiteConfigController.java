package selahattin.dev.ecom.controller.admin;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.admin.UpdateSiteAssetSlotRequest;
import selahattin.dev.ecom.dto.request.admin.UpdateSiteSettingRequest;
import selahattin.dev.ecom.dto.response.site.AssetResponse;
import selahattin.dev.ecom.entity.site.AssetEntity;
import selahattin.dev.ecom.repository.site.AssetRepository;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.SiteConfigService;
import selahattin.dev.ecom.service.infra.FileStorageService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminSiteConfigController {

    private final SiteConfigService siteConfigService;
    private final FileStorageService fileStorageService;
    private final AssetRepository assetRepository;

    @PatchMapping("/site-settings/{key}")
    @PreAuthorize("hasAuthority('site:manage')")
    public ResponseEntity<ApiResponse<Void>> updateSiteSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateSiteSettingRequest request) {

        siteConfigService.upsertSetting(key, request.getValue());
        return ResponseEntity.ok(ApiResponse.success("Site ayarı güncellendi"));
    }

    @PatchMapping("/site-assets/{slotKey}")
    @PreAuthorize("hasAuthority('site:manage')")
    public ResponseEntity<ApiResponse<Void>> updateSiteAssetSlot(
            @PathVariable String slotKey,
            @Valid @RequestBody UpdateSiteAssetSlotRequest request) {

        siteConfigService.upsertAssetSlot(slotKey, request.getAssetId());
        return ResponseEntity.ok(ApiResponse.success("Asset slotu güncellendi"));
    }

    @PostMapping(value = "/assets/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('site:manage')")
    public ResponseEntity<ApiResponse<AssetResponse>> uploadAsset(
            @RequestParam("file") MultipartFile file) {

        String url = fileStorageService.save(file);

        AssetEntity asset = AssetEntity.builder()
                .url(url)
                .objectKey(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown-file")
                .mimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .bytes(file.getSize())
                .build();

        AssetEntity saved = assetRepository.save(asset);

        AssetResponse response = AssetResponse.builder()
                .id(saved.getId())
                .url(saved.getUrl())
                .objectKey(saved.getObjectKey())
                .mimeType(saved.getMimeType())
                .bytes(saved.getBytes())
                .createdAt(saved.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Asset yüklendi", response));
    }
}
