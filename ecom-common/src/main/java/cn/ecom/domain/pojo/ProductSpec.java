package cn.ecom.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品规格模板表实体（如"颜色"及其可选值）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_product_spec")
public class ProductSpec implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 规格名(如 颜色/尺码) */
    private String specName;

    /** 可选值JSON数组(如 ["红","蓝"]) */
    private String specValues;

    private Integer sort;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
