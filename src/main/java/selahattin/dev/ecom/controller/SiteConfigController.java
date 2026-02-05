package selahattin.dev.ecom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.response.site.SiteConfigResponse;
import selahattin.dev.ecom.response.ApiResponse;
import selahattin.dev.ecom.service.domain.SiteConfigService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/site-config")
public class SiteConfigController {

    private final SiteConfigService siteConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<SiteConfigResponse>> getSiteConfig() {
        return ResponseEntity.ok(ApiResponse.success(
                "Site ayarları getirildi",
                siteConfigService.getPublicConfig()));
    }
}