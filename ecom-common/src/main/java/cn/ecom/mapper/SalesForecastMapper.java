package cn.ecom.mapper;

import cn.ecom.domain.pojo.SalesForecast;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SalesForecastMapper extends BaseMapper<SalesForecast> {

    /** 重跑前整体清空（物理删除，避免撞 uk_sku_date） */
    @Delete("DELETE FROM ec_sales_forecast")
    int deleteAll();

    /** 有预测数据的 SKU 列表（含商品标题/规格/28 天合计） */
    @Select("SELECT f.sku_id AS skuId, p.title AS productTitle, s.specs AS specs, " +
            "SUM(f.forecast_qty) AS total28 " +
            "FROM ec_sales_forecast f JOIN ec_sku s ON s.id = f.sku_id " +
            "JOIN ec_product p ON p.id = s.product_id " +
            "WHERE s.del_flag = 0 AND p.del_flag = 0 " +
            "GROUP BY f.sku_id, p.title, s.specs ORDER BY total28 DESC")
    List<Map<String, Object>> selectForecastSkuList();
}
