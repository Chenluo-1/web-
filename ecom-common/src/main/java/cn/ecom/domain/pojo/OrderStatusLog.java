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
 * 订单状态流转记录表实体
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_order_status_log")
public class OrderStatusLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Integer fromStatus;

    private Integer toStatus;

    /** 操作者类型(1用户 2管理员 3系统) */
    private Integer operatorType;

    private Long operatorId;

    private String remark;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
