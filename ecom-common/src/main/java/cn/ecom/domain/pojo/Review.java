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
 * 商品评价实体（简化实现：下单-收货后一次性文字+星级评价）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_review")
public class Review implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 订单明细id */
    private Long orderItemId;

    /** 评价人 */
    private Long userId;

    private Long productId;

    private Long skuId;

    /** 评分(1-5) */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 评价图片(JSON数组，简化版未启用) */
    private String images;

    /** 状态(0隐藏 1显示) */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
