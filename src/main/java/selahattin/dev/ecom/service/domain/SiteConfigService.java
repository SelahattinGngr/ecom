package selahattin.dev.ecom.service.domain;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.dto.response.site.AssetSlotResponse;
import selahattin.dev.ecom.dto.response.site.SiteConfigResponse;
import selahattin.dev.ecom.entity.site.AssetEntity;
import selahattin.dev.ecom.entity.site.SiteAssetSlotEntity;
import selahattin.dev.ecom.entity.site.SiteSettingEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.repository.site.AssetRepository;
import selahattin.dev.ecom.repository.site.SiteAssetSlotRepository;
import selahattin.dev.ecom.repository.site.SiteSettingRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private static final String CACHE_KEY = "site:public:config";
    private static final Duration CACHE_TTL = Duration.ofHours(2);

    private final SiteSettingRepository siteSettingRepository;
    private final SiteAssetSlotRepository siteAssetSlotRepository;
    private final AssetRepository assetRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public SiteConfigResponse getPublicConfig() {
        String cached = stringRedisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, SiteConfigResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("Site config cache deserialize hatası, DB'den yenileniyor.", e);
            }
        }

        SiteConfigResponse response = buildFromDb();

        try {
            stringRedisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(response), CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.error("Site config cache yazma hatası.", e);
        }

        return response;
    }

    public void invalidateCache() {
        stringRedisTemplate.delete(CACHE_KEY);
    }

    @Transactional
    public void upsertSetting(String key, String value) {
        SiteSettingEntity setting = siteSettingRepository.findBySettingKey(key)
                .orElseGet(() -> SiteSettingEntity.builder().settingKey(key).build());

        setting.setValueJson(Map.of("value", value));
        siteSettingRepository.save(setting);
        invalidateCache();
    }

    @Transactional
    public void upsertAssetSlot(String slotKey, UUID assetId) {
        AssetEntity asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Asset bulunamadı"));

        SiteAssetSlotEntity slot = siteAssetSlotRepository.findBySlotKey(slotKey)
                .orElseGet(() -> SiteAssetSlotEntity.builder().slotKey(slotKey).build());

        slot.setAsset(asset);
        siteAssetSlotRepository.save(slot);
        invalidateCache();
    }

    // --- PRIVATE ---

    private SiteConfigResponse buildFromDb() {
        List<SiteSettingEntity> settings = siteSettingRepository.findAll();
        List<SiteAssetSlotEntity> slots = siteAssetSlotRepository.findAll();

        Map<String, String> settingsMap = new LinkedHashMap<>();
        for (SiteSettingEntity s : settings) {
            Object val = s.getValueJson().get("value");
            settingsMap.put(s.getSettingKey(), val != null ? val.toString() : "");
        }

        Map<String, AssetSlotResponse> assetsMap = new LinkedHashMap<>();
        for (SiteAssetSlotEntity slot : slots) {
            if (slot.getAsset() != null) {
                assetsMap.put(slot.getSlotKey(), AssetSlotResponse.builder()
                        .url(slot.getAsset().getUrl())
                        .mimeType(slot.getAsset().getMimeType())
                        .build());
            }
        }

        return SiteConfigResponse.builder()
                .settings(settingsMap)
                .assets(assetsMap)
                .build();
    }
}
