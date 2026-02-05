package selahattin.dev.ecom.dto.response.site;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Getter
@Builder
public class SiteConfigResponse {
    private String siteName;
    private String siteDescription;
    private List<PaymentProvider> activePaymentMethods;

    // Stripe Frontend Key (Eğer Stripe aktifse Frontend'e lazım olabilir)
    private String stripePublishableKey;
}