package cn.ecom.mapper;

import cn.ecom.domain.pojo.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.Map;

public interface OrderMapper extends BaseMapper<Order> {

    /** 区间支付口径汇总：GMV + 支付订单数（不含未支付；已退款 status=6 不计入） */
    @Select("SELECT IFNULL(SUM(pay_amount),0) AS gmv, COUNT(*) AS cnt FROM ec_order " +
            "WHERE del_flag = 0 AND status IN (1,2,3,5) AND pay_time IS NOT NULL " +
            "AND pay_time >= #{from} AND pay_time < #{to}")
    Map<String, Object> selectPaidSummary(@Param("from") Date from, @Param("to") Date to);

    /** 区间下单数（含未支付） */
    @Select("SELECT COUNT(*) FROM ec_order WHERE del_flag = 0 AND create_time >= #{from} AND create_time < #{to}")
    Integer countCreated(@Param("from") Date from, @Param("to") Date to);

    /** 区间已退款订单数 */
    @Select("SELECT COUNT(*) FROM ec_order WHERE del_flag = 0 AND status = 6 " +
            "AND pay_time >= #{from} AND pay_time < #{to}")
    Integer countRefunded(@Param("from") Date from, @Param("to") Date to);
}
