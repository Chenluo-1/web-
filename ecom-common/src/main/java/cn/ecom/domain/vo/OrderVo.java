package cn.ecom.domain.vo;

import cn.ecom.domain.pojo.Order;
import cn.ecom.domain.pojo.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单 VO（订单 + 明细 + 状态文案）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVo {
    private Order order;
    private List<OrderItem> items;
    private String statusName;

    public static String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "待支付";
            case 1: return "待发货";
            case 2: return "运输中";
            case 3: return "已完成";
            case 4: return "已取消";
            case 5: return "退款中";
            case 6: return "已退款";
            default: return "未知";
        }
    }
}
