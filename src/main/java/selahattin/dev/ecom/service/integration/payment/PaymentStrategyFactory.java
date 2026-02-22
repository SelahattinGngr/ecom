package selahattin.dev.ecom.service.integration.payment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import selahattin.dev.ecom.exception.BusinessException;
import selahattin.dev.ecom.exception.ErrorCode;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

@Component
public class PaymentStrategyFactory {

    private final Map<PaymentProvider, PaymentProviderStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentProviderStrategy> strategyList) {
        this.strategies = new EnumMap<>(PaymentProvider.class);
        for (PaymentProviderStrategy strategy : strategyList) {
            this.strategies.put(strategy.getProviderName(), strategy);
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