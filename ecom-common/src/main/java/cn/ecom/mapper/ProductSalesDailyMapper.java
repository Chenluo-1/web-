package cn.ecom.mapper;

import cn.ecom.domain.pojo.ProductSalesDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ProductSalesDailyMapper extends BaseMapper<ProductSalesDaily> {

    @Delete("DELETE FROM ec_product_sales_daily WHERE stat_date = #{statDate}")
    int deleteByDate(@Param("statDate") Date statDate);

    /** 热销 TOP10（区间内销量降序） */
    @Select("SELECT s.product_id AS productId, p.title AS title, SUM(s.sales_qty) AS qty, " +
            "IFNULL(SUM(s.sales_amount),0) AS amount " +
            "FROM ec_product_sales_daily s JOIN ec_product p ON p.id = s.product_id " +
            "WHERE s.stat_date >= #{from} AND s.stat_date <= #{to} " +
            "GROUP BY s.product_id, p.title ORDER BY qty DESC LIMIT 10")
    List<Map<String, Object>> selectHotTop(@Param("from") Date from, @Param("to") Date to);

    /** 滞销 TOP10（区间内销量升序，含 0 销量商品） */
    @Select("SELECT p.id AS productId, p.title AS title, IFNULL(SUM(s.sales_qty),0) AS qty " +
            "FROM ec_product p LEFT JOIN ec_product_sales_daily s ON s.product_id = p.id " +
            "AND s.stat_date >= #{from} AND s.stat_date <= #{to} " +
            "WHERE p.del_flag = 0 AND p.status = 1 " +
            "GROUP BY p.id, p.title ORDER BY qty ASC, p.id ASC LIMIT 10")
    List<Map<String, Object>> selectColdTop(@Param("from") Date from, @Param("to") Date to);

    /** 区间内各商品销售额（类目占比用，按商品归集后 Java 侧向上归到一级类目） */
    @Select("SELECT s.product_id AS productId, SUM(s.sales_amount) AS amount " +
            "FROM ec_product_sales_daily s " +
            "WHERE s.stat_date >= #{from} AND s.stat_date <= #{to} " +
            "GROUP BY s.product_id")
    List<Map<String, Object>> selectProductAmount(@Param("from") Date from, @Param("to") Date to);
}
