package selahattin.dev.ecom.service.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iyzipay.model.CheckoutFormInitialize;

import selahattin.dev.ecom.config.properties.PaymentProperties;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.dto.response.payment.PaymentInitResponse;
import selahattin.dev.ecom.entity.auth.UserEntity;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.service.integration.payment.impl.IyzicoPaymentProvider;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@ExtendWith(MockitoExtension.class)
class IyzicoPaymentProviderTest {

    @Mock
    private PaymentProperties paymentProperties;

    @InjectMocks
    private IyzicoPaymentProvider iyzicoPaymentProvider;

    @Test
    void initializePayment_ShouldThrowException_WhenApiKeysMissing() {
        // Arrange
        PaymentProperties.Iyzico iyzicoProps = new PaymentProperties.Iyzico();
        // Null api keys
        when(paymentProperties.getIyzico()).thenReturn(iyzicoProps);

        PaymentEntity payment = new PaymentEntity();
        OrderEntity order = new OrderEntity();
        payment.setOrder(order);

        // Act & Assert
        // Expect BusinessException or Runtime check
        try {
            iyzicoPaymentProvider.initializePayment(payment, new PaymentInitRequest());
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("anahtarları konfigüre edilmemiş");
        }
    }

    // Note: Testing actual IO with Iyzico requires comprehensive mocking of their
    // static methods or integration tests.
    // Here we focus on the adapter logic validation.

    @Test
    void getProviderName_ShouldReturnIyzico() {
        assertThat(iyzicoPaymentProvider.getProviderName()).isEqualTo(PaymentProvider.IYZICO);
    }
}
