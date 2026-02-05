package selahattin.dev.ecom.service.domain;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.config.properties.PaymentProperties;
import selahattin.dev.ecom.dto.response.site.SiteConfigResponse;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private final PaymentProperties paymentProperties;

    private static final String SITE_NAME = "Selahattin E-Com";
    private static final String SITE_DESC = "En iyi ürünler, en iyi fiyatlar.";

    public SiteConfigResponse getPublicConfig() {
        // Stripe aktif mi kontrol et?
        boolean isStripeActive = paymentProperties.getEnabledProviders().contains(PaymentProvider.STRIPE);
        String stripeKey = isStripeActive ? paymentProperties.getStripe().getPublishableKey() : null;

        return SiteConfigResponse.builder()
                .siteName(SITE_NAME)
                .siteDescription(SITE_DESC)
                .activePaymentMethods(paymentProperties.getEnabledProviders())
                .stripePublishableKey(stripeKey) // Stripe.js için gerekli
                .build();
    }
}