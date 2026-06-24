package selahattin.dev.ecom.service.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import selahattin.dev.ecom.dto.request.payment.PaymentInitRequest;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.payment.PaymentEntity;
import selahattin.dev.ecom.repository.payment.PaymentRepository;
import selahattin.dev.ecom.utils.enums.PaymentProvider;
import selahattin.dev.ecom.utils.enums.PaymentStatus;

/**
 * Payment kaydını kendi bağımsız transaction'ında (REQUIRES_NEW) oluşturup
 * hemen commit eder.
 *
 * Neden ayrı sınıf: PaymentService.initPayment içinden bu metodu çağırıp
 * PaymentEventService.log (REQUIRES_NEW) çağırdığımızda, event log'unun
 * görebileceği bir payment satırı olması gerekiyor. Ama bu metodu
 * PaymentService
 * kendi içinde tanımlarsak self-invocation olur
 * (this.createPendingPayment(...)),
 * Spring'in proxy'si atlanır ve @Transactional hiç devreye girmez, ana
 * transaction'ın içinde kalır. Ayrı bean olduğu için dışarıdan proxy üzerinden
 * çağrılıyor ve REQUIRES_NEW gerçekten çalışıyor.
 */
@Service
@RequiredArgsConstructor
public class PaymentInitiator {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentEntity createPendingPayment(OrderEntity order, PaymentInitRequest request,
            PaymentProvider activeProvider) {

        PaymentEntity payment = PaymentEntity.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentProvider(activeProvider)
                .status(PaymentStatus.PENDING)
                .description("Sipariş ödemesi #" + order.getId())
                .clientIp(request.getClientIp())
                .build();

        return paymentRepository.save(payment);
    }
}