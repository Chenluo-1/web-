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
 * 系统参数表实体（规则阈值全部配置化）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_sys_config")
public class SysConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configKey;

    private String configName;

    private String configValue;

    private String remark;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
