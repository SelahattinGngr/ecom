package selahattin.dev.ecom.service.integration.payment;

import java.math.BigDecimal;

import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

/**
 * Tüm ödeme sağlayıcıların (Stripe, Iyzico, Garanti, Mock) uyması gereken ANA
 * SÖZLEŞME.
 */
public interface PaymentProviderStrategy {

    /**
     * Hangi sağlayıcı olduğunu döner (Enum).
     */
    PaymentProvider getProviderName();

    /**
     * 1. ÖDEME BAŞLATMA (Initialize/Auth)
     * Kullanıcıyı 3D Secure sayfasına veya ödeme formuna yönlendirmek için link
     * üretir.
     */
    PaymentInitResponse initializePayment(PaymentEntity payment, PaymentInitRequest request);

    /**
     * 2. TAHSİLAT (Capture)
     * "Provizyon"daki (bekleyen) parayı resmen çeker.
     * Genelde kargoya verilirken kullanılır.
     *
     * @param payment Veritabanındaki ödeme kaydı
     */
    void capturePayment(PaymentEntity payment);

    /**
     * 3. İPTAL (Void)
     * Gün sonu kapanmadan önce işlemi iptal eder. Para karttan hiç çekilmemiş gibi
     * olur.
     *
     * @param payment Veritabanındaki ödeme kaydı
     */
    void voidPayment(PaymentEntity payment);

    /**
     * 4. İADE (Refund)
     * Para çekildikten sonra (Capture sonrası) iade işlemi yapar.
     *
     * @param payment      Veritabanındaki ödeme kaydı
     * @param refundAmount İade edilecek tutar (Kısmi iade olabilir)
     */
    void refundPayment(PaymentEntity payment, BigDecimal refundAmount);
}