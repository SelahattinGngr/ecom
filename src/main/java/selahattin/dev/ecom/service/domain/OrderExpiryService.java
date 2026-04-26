package selahattin.dev.ecom.service.domain;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import selahattin.dev.ecom.entity.order.OrderEntity;
import selahattin.dev.ecom.entity.order.OrderItemEntity;
import selahattin.dev.ecom.repository.catalog.ProductVariantRepository;
import selahattin.dev.ecom.repository.order.OrderRepository;
import selahattin.dev.ecom.utils.enums.OrderStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExpiryService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(15);
        List<OrderEntity> expiredOrders = orderRepository.findExpiredPendingOrders(threshold);

        if (expiredOrders.isEmpty()) {
            return;
        }

        for (OrderEntity order : expiredOrders) {
            for (OrderItemEntity item : order.getItems()) {
                productVariantRepository.increaseStock(item.getProductVariant().getId(), item.getQuantity());
            }
            order.setStatus(OrderStatus.CANCELLED);
            log.warn("[EXPIRY] PENDING sipariş otomatik iptal edildi. OrderId: {}, CreatedAt: {}",
                    order.getId(), order.getCreatedAt());
        }

        orderRepository.saveAll(expiredOrders);
    }
}
