package cn.ecom.mapper;

import cn.ecom.domain.pojo.OrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 某 SKU 的日销量聚合（已支付且未退款的订单，按支付日期）
     * 返回：[{sales_date: '2026-09-15', qty: 3}, ...]
     */
    @Select("SELECT DATE(o.pay_time) AS sales_date, SUM(i.quantity) AS qty " +
            "FROM ec_order_item i JOIN ec_order o ON o.order_no = i.order_no " +
            "WHERE i.sku_id = #{skuId} AND i.del_flag = 0 AND o.del_flag = 0 " +
            "AND o.status IN (1, 2, 3) AND o.pay_time IS NOT NULL AND o.pay_time >= #{since} " +
            "GROUP BY DATE(o.pay_time)")
    List<Map<String, Object>> selectDailySales(@Param("skuId") Long skuId, @Param("since") Date since);

    /** 某类目下（除指定 SKU 外）全部 SKU 的日销量聚合（新品兜底用） */
    @Select("SELECT i.sku_id AS sku_id, SUM(i.quantity) AS qty, COUNT(DISTINCT DATE(o.pay_time)) AS days " +
            "FROM ec_order_item i JOIN ec_order o ON o.order_no = i.order_no " +
            "JOIN ec_sku s ON s.id = i.sku_id JOIN ec_product p ON p.id = s.product_id " +
            "WHERE p.category_id = #{categoryId} AND s.id <> #{excludeSkuId} " +
            "AND i.del_flag = 0 AND o.del_flag = 0 AND s.del_flag = 0 AND p.del_flag = 0 " +
            "AND o.status IN (1, 2, 3) AND o.pay_time IS NOT NULL AND o.pay_time >= #{since} " +
            "GROUP BY i.sku_id")
    List<Map<String, Object>> selectCategoryDailySales(@Param("categoryId") Long categoryId,
                                                       @Param("excludeSkuId") Long excludeSkuId,
                                                       @Param("since") Date since);

    /** 区间支付口径的商品/SKU 销量汇总（T+1 汇总表来源） */
    @Select("SELECT i.product_id AS productId, i.sku_id AS skuId, SUM(i.quantity) AS qty, " +
            "SUM(i.subtotal) AS amount " +
            "FROM ec_order_item i JOIN ec_order o ON o.order_no = i.order_no " +
            "JOIN ec_product p ON p.id = i.product_id AND p.del_flag = 0 " +
            "WHERE o.status IN (1,2,3,5) AND o.pay_time >= #{from} AND o.pay_time < #{to} " +
            "AND o.del_flag = 0 AND i.del_flag = 0 " +
            "GROUP BY i.product_id, i.sku_id")
    List<Map<String, Object>> selectPaidItemSummary(@Param("from") Date from, @Param("to") Date to);
}
