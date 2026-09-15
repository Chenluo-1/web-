package cn.ecom.domain.vo;

import cn.ecom.domain.pojo.Logistics;
import cn.ecom.domain.pojo.Order;
import cn.ecom.domain.pojo.OrderItem;
import cn.ecom.domain.pojo.OrderStatusLog;
import cn.ecom.domain.pojo.Refund;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单详情 VO：订单 + 明细 + 状态时间轴 + 物流 + 退款单
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailVo {
    private Order order;
    private List<OrderItem> items;
    private String statusName;
    private List<OrderStatusLog> logs;
    private Logistics logistics;
    private Refund refund;
}
