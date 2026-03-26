package selahattin.dev.ecom.controller;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.service.domain.PaymentService;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    /**
     * Iyzico form urlencoded formatında POST atar.
     * İşlem bittikten sonra 302 döndürerek müşteriyi Next.js sayfasına yollarız.
     */
    @PostMapping(value = "/{provider}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleFormCallback(
            @PathVariable String provider,
            @RequestParam Map<String, String> payload) {

        PaymentProvider paymentProvider;
        try {
            log.info("[WEBHOOK] Callback alındı. Provider: {}", provider.toUpperCase(Locale.ENGLISH));
            paymentProvider = PaymentProvider.valueOf(provider.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Geçersiz ödeme sağlayıcısı: " + provider);
        }

        String redirectUrl = paymentService.processWebhook(paymentProvider, payload);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
