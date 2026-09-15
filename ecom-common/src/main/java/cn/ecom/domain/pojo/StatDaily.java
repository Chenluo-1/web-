package cn.ecom.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 日汇总统计实体（uk: stat_date）
 * 不映射 del_flag：T+1 重算按"物理删除当日 + 重插"覆盖
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_stat_daily")
public class StatDaily implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Date statDate;

    /** GMV（支付成功口径，不含未支付/已退款） */
    private BigDecimal gmv;

    private Integer orderCount;

    private Integer paidCount;

    private Integer refundCount;

    /** 访客数（行为去重用户） */
    private Integer visitorCount;

    /** 浏览量 */
    private Integer pv;

    private Integer cartCount;

    private Date createTime;

    private Date updateTime;
}
