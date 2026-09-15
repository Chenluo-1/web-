package cn.ecom.task;

import cn.ecom.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时任务：每分钟扫描超时未支付订单，自动取消并释放库存、退回优惠券
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    private final OrderService orderService;

    public OrderTimeoutTask(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void cancelExpiredOrders() {
        try {
            int count = orderService.cancelExpiredOrders();
            if (count > 0) {
                log.info("订单超时任务：本次取消 {} 笔订单", count);
            }
        } catch (Exception e) {
            log.error("订单超时任务执行异常", e);
        }
    }
}
