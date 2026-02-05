package selahattin.dev.ecom.service.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@ExtendWith(MockitoExtension.class)
class PaymentStrategyFactoryTest {

    private PaymentStrategyFactory factory;
    private PaymentProviderStrategy mockStrategy;

    @BeforeEach
    void setUp() {
        mockStrategy = mock(PaymentProviderStrategy.class);
        when(mockStrategy.getProviderName()).thenReturn(PaymentProvider.MOCK);

        List<PaymentProviderStrategy> strategies = Collections.singletonList(mockStrategy);
        factory = new PaymentStrategyFactory(strategies);
    }

    @Test
    void getStrategy_ShouldReturnCorrectStrategy_WhenProviderExists() {
        // Act
        PaymentProviderStrategy strategy = factory.getStrategy(PaymentProvider.MOCK);

        // Assert
        assertThat(strategy).isEqualTo(mockStrategy);
    }

    @Test
    void getStrategy_ShouldThrowException_WhenProviderDoesNotExist() {
        // Act & Assert
        assertThatThrownBy(() -> factory.getStrategy(PaymentProvider.STRIPE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entegre edilmemiş");
    }
}
