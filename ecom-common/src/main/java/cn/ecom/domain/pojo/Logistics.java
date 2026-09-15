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
 * 物流单实体（status：0已揽收 1运输中 2派送中 3已签收）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_logistics")
public class Logistics implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String company;

    private String trackingNo;

    private Integer status;

    /** 物流轨迹JSON（[{time, text}] 数组，按时间推进） */
    private String traceJson;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
