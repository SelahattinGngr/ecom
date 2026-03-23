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

    private PaymentProvider activeProvider = PaymentProvider.MOCK;

    private List<PaymentProvider> enabledProviders = new ArrayList<>(List.of(PaymentProvider.MOCK));

    private Stripe stripe = new Stripe();
    private Iyzico iyzico = new Iyzico();

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
}