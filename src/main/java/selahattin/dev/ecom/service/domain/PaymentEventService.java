package selahattin.dev.ecom.service.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.entity.payment.PaymentEventEntity;
import selahattin.dev.ecom.repository.payment.PaymentEventRepository;
import selahattin.dev.ecom.utils.enums.PaymentEventType;
import selahattin.dev.ecom.utils.enums.PaymentProvider;

/**
 * Ödeme akışındaki kritik adımları veritabanına yazar.
 *
 * REQUIRES_NEW: Ana transaction rollback olsa dahi event kaydı korunur.
 * Özellikle WEBHOOK_RECEIVED için kritik: provider verisinin itiraz
 * süreçlerinde kanıt niteliği taşıması gerekir.
 *
 * Yazma hatası dış akışı kesmemesi için çağrı noktalarında try-catch ile sarılır.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventService {

    private final PaymentEventRepository paymentEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID paymentId,
                    PaymentEventType eventType,
                    PaymentProvider provider,
                    Map<String, Object> payload) {

        PaymentEventEntity event = PaymentEventEntity.builder()
                .paymentId(paymentId)
                .eventType(eventType.name())
                .provider(provider != null ? provider.name() : null)
                .rawPayload(payload)
                .build();

        paymentEventRepository.save(event);
    }

    public List<PaymentEventEntity> getEventsForPayment(UUID paymentId) {
        return paymentEventRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId);
    }
}
