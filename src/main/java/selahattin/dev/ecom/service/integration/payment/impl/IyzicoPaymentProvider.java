package selahattin.dev.ecom.service.integration.payment.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.config.properties.PaymentProperties;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.order.OrderItemEntity;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.service.integration.payment.PaymentProviderStrategy;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class IyzicoPaymentProvider implements PaymentProviderStrategy {

    private final PaymentProperties paymentProperties;

    private static final String STATUS_SUCCESS = "success";
    private static final String DEFAULT_IP = "127.0.0.1";

    @Override
    public PaymentProvider getProviderName() {
        return PaymentProvider.IYZICO;
    }

    private Options getOptions() {
        PaymentProperties.Iyzico iyzicoProps = paymentProperties.getIyzico();

        // FAIL-FAST: Eğer anahtarlar yoksa BusinessException fırlatıyoruz.
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
        // Önce seçenekleri alarak kontrolü en başta yapıyoruz (Testin geçmesi için
        // kritik)
        Options options = getOptions();

        // Güvenli Loglama: payment veya order null olsa bile patlamaz.
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

        // 1. BUYER
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

        // 2. ADDRESS
        Address address = new Address();
        address.setContactName(buyer.getName() + " " + buyer.getSurname());
        address.setCity(city);
        address.setCountry(country);
        address.setAddress(addressText);
        address.setZipCode("34000");
        iyzicoRequest.setShippingAddress(address);
        iyzicoRequest.setBillingAddress(address);

        // 3. BASKET
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
            // Kendi hatamızı tekrar fırlatıyoruz ki mesaj değişmesin
            throw be;
        } catch (Exception e) {
            log.error("[IYZICO] Exception: ", e);
            throw new BusinessException(ErrorCode.PAYMENT_INIT_ERROR, "Iyzico bağlantı hatası");
        }
    }

    @Override
    public void capturePayment(PaymentEntity payment) {
        log.info("[IYZICO] Capture işlemi otomatik yapıldı.");
    }

    @Override
    public void voidPayment(PaymentEntity payment) {
        log.info("[IYZICO] İptal (Cancel) işlemi. ID: {}", payment.getId());
        try {
            CreateCancelRequest request = new CreateCancelRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId(payment.getId().toString());
            request.setPaymentId(payment.getPaymentTransactionId());
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
        log.info("[IYZICO] İade (Refund) işlemi. ID: {}", payment.getId());
        try {
            CreateRefundRequest request = new CreateRefundRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId(payment.getId().toString());
            request.setPaymentTransactionId(payment.getPaymentTransactionId());
            request.setPrice(refundAmount);
            request.setIp(DEFAULT_IP);
            request.setCurrency(Currency.TRY.name());

            Refund refund = Refund.create(request, getOptions());
            if (!STATUS_SUCCESS.equals(refund.getStatus())) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                        "Iyzico İade Hatası: " + refund.getErrorMessage());
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "Iyzico iade hatası: " + e.getMessage());
        }
    }
}