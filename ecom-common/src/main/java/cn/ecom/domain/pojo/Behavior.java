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
 * 用户行为实体（expose曝光 click点击 cart加购 order下单 pay支付）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_user_behavior")
public class Behavior implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户id（游客为空） */
    private Long userId;

    /** 事件类型 */
    private String eventType;

    /** 目标类型(product/sku/category) */
    private String targetType;

    private Long targetId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
