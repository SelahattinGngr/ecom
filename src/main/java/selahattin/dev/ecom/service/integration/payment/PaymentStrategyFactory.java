package selahattin.dev.ecom.service.integration.payment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Component
@RequiredArgsConstructor
public class PaymentStrategyFactory {

    private final Map<PaymentProvider, PaymentProviderStrategy> strategies = new EnumMap<>(PaymentProvider.class);

    // TÜM sınıfları (Mock, Stripe, Iyzico...) bulup bu listeye otomatik doldurur.
    public PaymentStrategyFactory(List<PaymentProviderStrategy> strategyList) {
        for (PaymentProviderStrategy strategy : strategyList) {
            strategies.put(strategy.getProviderName(), strategy);
        }
    }

    public PaymentProviderStrategy getStrategy(PaymentProvider provider) {
        PaymentProviderStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Bu ödeme yöntemi henüz entegre edilmemiş: " + provider);
        }
        return strategy;
    }
}