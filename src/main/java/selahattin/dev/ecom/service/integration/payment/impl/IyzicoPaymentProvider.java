package selahattin.dev.ecom.service.integration.payment.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.config.properties.PaymentProperties;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentCallbackResult;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.order.OrderItemEntity;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.service.integration.payment.PaymentProviderStrategy;
import selahattin.dev.ecom.utils.enums.PaymentProvider;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class IyzicoPaymentProvider implements PaymentProviderStrategy {

    private final PaymentProperties paymentProperties;

    private static final String STATUS_SUCCESS = "success";
    private static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    private static final String DEFAULT_IP = "127.0.0.1";

    @Override
    public PaymentProvider getProviderName() {
        return PaymentProvider.IYZICO;
    }

    private Options getOptions() {
        PaymentProperties.Iyzico iyzicoProps = paymentProperties.getIyzico();
        if (iyzicoProps.getApiKey() == null || iyzicoProps.getSecretKey() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Iyzico API anahtarları konfigüre edilmemiş!");
        }
        Options options = new Options();
        options.setApiKey(iyzicoProps.getApiKey());
        options.setSecretKey(iyzicoProps.getSecretKey());
        options.setBaseUrl(iyzicoProps.getBaseUrl());
        return options;
    }

    @Override
    public PaymentInitResponse initializePayment(PaymentEntity payment, PaymentInitRequest request) {
        Options options = getOptions();

        String orderId = Optional.ofNullable(payment.getOrder())
                .map(order -> order.getId().toString())
                .orElse("UNKNOWN");
        log.info("[IYZICO] Ödeme formu oluşturuluyor. Order ID: {}", orderId);

        OrderEntity order = payment.getOrder();
        UserEntity user = order.getUser();
        PaymentProperties.Iyzico iyzicoProps = paymentProperties.getIyzico();

        CreateCheckoutFormInitializeRequest iyzicoRequest = new CreateCheckoutFormInitializeRequest();
        iyzicoRequest.setLocale(Locale.TR.getValue());
        iyzicoRequest.setConversationId(payment.getId().toString());
        iyzicoRequest.setPrice(payment.getAmount());
        iyzicoRequest.setPaidPrice(payment.getAmount());
        iyzicoRequest.setCurrency(Currency.TRY.name());
        iyzicoRequest.setBasketId(order.getId().toString());
        iyzicoRequest.setPaymentGroup(PaymentGroup.PRODUCT.name());
        iyzicoRequest.setCallbackUrl(iyzicoProps.getCallbackUrl());
        iyzicoRequest.setEnabledInstallments(Arrays.asList(2, 3, 6, 9));

        Buyer buyer = new Buyer();
        buyer.setId(user.getId().toString());
        buyer.setName(user.getFirstName());
        buyer.setSurname(user.getLastName());
        buyer.setGsmNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "+905555555555");
        buyer.setEmail(user.getEmail());
        buyer.setIdentityNumber("11111111111");
        buyer.setLastLoginDate("2024-01-01 12:00:00");
        buyer.setRegistrationDate("2024-01-01 12:00:00");

        String addressText = "Teslimat Adresi";
        String city = "Istanbul";
        String country = "Turkey";

        if (order.getShippingAddress() != null) {
            addressText = (String) order.getShippingAddress().getOrDefault("fullAddress", addressText);
            city = (String) order.getShippingAddress().getOrDefault("cityName", city);
        }

        buyer.setRegistrationAddress(addressText);
        buyer.setIp(DEFAULT_IP);
        buyer.setCity(city);
        buyer.setCountry(country);
        iyzicoRequest.setBuyer(buyer);

        Address address = new Address();
        address.setContactName(buyer.getName() + " " + buyer.getSurname());
        address.setCity(city);
        address.setCountry(country);
        address.setAddress(addressText);
        address.setZipCode("34000");
        iyzicoRequest.setShippingAddress(address);
        iyzicoRequest.setBillingAddress(address);

        List<BasketItem> basketItems = new ArrayList<>();
        for (OrderItemEntity item : order.getItems()) {
            BasketItem basketItem = new BasketItem();
            basketItem.setId(item.getProductVariant().getId().toString());
            basketItem.setName(item.getProductNameAtPurchase());
            basketItem.setCategory1("General");
            basketItem.setItemType(BasketItemType.PHYSICAL.name());
            basketItem.setPrice(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));
            basketItems.add(basketItem);
        }
        iyzicoRequest.setBasketItems(basketItems);

        try {
            CheckoutFormInitialize checkoutForm = CheckoutFormInitialize.create(iyzicoRequest, options);

            if (!STATUS_SUCCESS.equals(checkoutForm.getStatus())) {
                log.error("[IYZICO] Init Hatası: Code: {}, Msg: {}", checkoutForm.getErrorCode(),
                        checkoutForm.getErrorMessage());
                throw new BusinessException(ErrorCode.PAYMENT_INIT_ERROR,
                        "Iyzico Hatası: " + checkoutForm.getErrorMessage());
            }

            payment.setPaymentTransactionId(checkoutForm.getToken());

            return PaymentInitResponse.builder()
                    .paymentId(payment.getId())
                    .redirectUrl(checkoutForm.getPaymentPageUrl())
                    .htmlContent(checkoutForm.getCheckoutFormContent())
                    .build();

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[IYZICO] Exception: ", e);
            throw new BusinessException(ErrorCode.PAYMENT_INIT_ERROR, "Iyzico bağlantı hatası");
        }
    }

    @Override
    public PaymentCallbackResult processCallback(Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null) {
            log.error("[IYZICO] Callback isteğinde token bulunamadı!");
            return PaymentCallbackResult.builder().status(PaymentStatus.FAILED).errorCode("NO_TOKEN").build();
        }

        try {
            log.info("[IYZICO] Ödeme sonucu sorgulanıyor. Token: {}", token);
            RetrieveCheckoutFormRequest request = new RetrieveCheckoutFormRequest();
            request.setToken(token);

            CheckoutForm form = CheckoutForm.retrieve(request, getOptions());

            PaymentStatus finalStatus = PaymentStatus.FAILED;
            if (STATUS_SUCCESS.equals(form.getStatus()) && PAYMENT_STATUS_SUCCESS.equals(form.getPaymentStatus())) {
                finalStatus = PaymentStatus.SUCCEEDED;
                log.info("[IYZICO] Ödeme BAŞARILI. Token: {}", token);
            } else {
                log.warn("[IYZICO] Ödeme BAŞARISIZ. Msg: {}", form.getErrorMessage());
            }

            // Başarılı ödemede iade için gerekli per-item transaction ID'leri topla
            List<String> itemTransactionIds = null;
            if (finalStatus == PaymentStatus.SUCCEEDED && form.getPaymentItems() != null) {
                itemTransactionIds = form.getPaymentItems().stream()
                        .map(com.iyzipay.model.PaymentItem::getPaymentTransactionId)
                        .filter(id -> id != null && !id.isBlank())
                        .toList();
            }

            return PaymentCallbackResult.builder()
                    .transactionId(token)
                    .status(finalStatus)
                    .errorCode(form.getErrorCode() != null ? form.getErrorCode() : form.getErrorMessage())
                    .providerPaymentId(form.getPaymentId())
                    .itemTransactionIds(itemTransactionIds)
                    .build();

        } catch (Exception e) {
            log.error("[IYZICO] Callback Retrieve Exception: ", e);
            return PaymentCallbackResult.builder()
                    .transactionId(token)
                    .status(PaymentStatus.FAILED)
                    .errorCode("EXCEPTION_OCCURRED")
                    .build();
        }
    }

    @Override
    public void capturePayment(PaymentEntity payment) {
        log.info("[IYZICO] Capture işlemi otomatik yapıldı.");
    }

    @Override
    public void voidPayment(PaymentEntity payment) {
        log.info("[IYZICO] İptal (Cancel) işlemi. Payment ID: {}", payment.getId());
        try {
            String numericPaymentId = resolveNumericPaymentId(payment);

            CreateCancelRequest request = new CreateCancelRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId(payment.getId().toString());
            request.setPaymentId(numericPaymentId);
            request.setIp(DEFAULT_IP);

            Cancel cancel = Cancel.create(request, getOptions());
            if (!STATUS_SUCCESS.equals(cancel.getStatus())) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                        "Iyzico İptal Hatası: " + cancel.getErrorMessage());
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "Iyzico iptal hatası: " + e.getMessage());
        }
    }

    @Override
    public void refundPayment(PaymentEntity payment, BigDecimal refundAmount) {
        log.info("[IYZICO] İade (Refund) işlemi. Payment ID: {}", payment.getId());
        try {
            List<String> itemTxIds = payment.getProviderItemTransactionIds();
            if (itemTxIds == null || itemTxIds.isEmpty()) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                        "Iyzico iade için ödeme kalemi transaction ID'leri bulunamadı. " +
                        "Bu ödeme webhook callback'i işlenirken kaydedilmemiş olabilir.");
            }

            // Her ödeme kalemi için ayrı iade isteği gönder
            for (String itemTxId : itemTxIds) {
                CreateRefundRequest request = new CreateRefundRequest();
                request.setLocale(Locale.TR.getValue());
                request.setConversationId(payment.getId().toString());
                request.setPaymentTransactionId(itemTxId);
                request.setPrice(refundAmount.divide(
                        java.math.BigDecimal.valueOf(itemTxIds.size()),
                        2, java.math.RoundingMode.HALF_UP));
                request.setIp(DEFAULT_IP);
                request.setCurrency(Currency.TRY.name());

                Refund refund = Refund.create(request, getOptions());
                if (!STATUS_SUCCESS.equals(refund.getStatus())) {
                    throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                            "Iyzico İade Hatası (item: " + itemTxId + "): " + refund.getErrorMessage());
                }
                log.info("[IYZICO] Kalem iade edildi. paymentTransactionId: {}", itemTxId);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "Iyzico iade hatası: " + e.getMessage());
        }
    }

    /**
     * Iyzico'nun numeric paymentId'sini döner (Cancel işlemi için).
     * providerPaymentId callback'te kaydedilir.
     */
    private String resolveNumericPaymentId(PaymentEntity payment) {
        if (payment.getProviderPaymentId() != null) {
            return payment.getProviderPaymentId();
        }
        throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                "Iyzico iptal için provider_payment_id bulunamadı. " +
                "Bu ödeme webhook callback'i işlenirken kaydedilmemiş olabilir.");
    }
}