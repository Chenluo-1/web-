package cn.ecom.mapper;

import cn.ecom.domain.pojo.Sku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SkuMapper extends BaseMapper<Sku> {

    /**
     * 原子扣减库存（防超卖）：库存不足时影响行数为 0。
     * 自定义 SQL 不走 MP 逻辑删除，手动带 del_flag 条件。
     */
    @Update("UPDATE ec_sku SET stock = stock - #{quantity}, version = version + 1, update_time = NOW() " +
            "WHERE id = #{skuId} AND stock >= #{quantity} AND del_flag = 0")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /** 回补库存（取消/超时释放） */
    @Update("UPDATE ec_sku SET stock = stock + #{quantity}, update_time = NOW() " +
            "WHERE id = #{skuId} AND del_flag = 0")
    int restoreStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
