package cn.ecom.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 销量预测表实体（uk: sku_id + forecast_date）
 * 不映射 del_flag：重跑按"物理删除+重插"覆盖（避免撞唯一键）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_sales_forecast")
public class SalesForecast implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skuId;

    private Date forecastDate;

    private Integer forecastQty;

    private Date createTime;

    private Date updateTime;
}
