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
 * 收货地址表实体
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_address")
public class Address implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String receiverName;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String detail;

    /** 默认地址(0否 1是) */
    private Integer isDefault;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
