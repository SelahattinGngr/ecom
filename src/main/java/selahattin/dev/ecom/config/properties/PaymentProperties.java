package selahattin.dev.ecom.config.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Data;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "selahattin.dev.payment")
public class PaymentProperties {

    /**
     * Aktif ödeme sağlayıcısı (MOCK, STRIPE, IYZICO, GARANTI)
     * (Bu backend'in development varsayılanıdır, runtime'da request belirler)
     */
    private PaymentProvider activeProvider = PaymentProvider.MOCK;

    /**
     * Frontend'de gösterilecek aktif ödeme yöntemleri listesi.
     * application.properties üzerinden yönetilir.
     */
    private List<PaymentProvider> enabledProviders = new ArrayList<>(List.of(PaymentProvider.MOCK));

    private Stripe stripe = new Stripe();
    private Iyzico iyzico = new Iyzico();
    private Garanti garanti = new Garanti();

    @Data
    public static class Stripe {
        private String apiKey;
        private String publishableKey;
        private String webhookSecret;
    }

    @Data
    public static class Iyzico {
        private String apiKey;
        private String secretKey;
        private String baseUrl;
        private String callbackUrl;
    }

    @Data
    public static class Garanti {
        private String merchantId;
        private String merchantPassword;
        private String terminalId;
        private String storeKey;
        private String provisionUser;
        private String provisionPassword;
        private String baseUrl;
        private String callbackUrl;
    }
}