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
 * 操作日志表实体（写操作审计）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块名 */
    private String module;

    /** 操作类型（新增/修改/删除等） */
    private String operType;

    /** 方法名 */
    private String method;

    /** 请求参数（密码已脱敏） */
    private String params;

    /** 操作人id */
    private Long operatorId;

    /** 操作人登录名 */
    private String operatorName;

    /** 操作 IP */
    private String ip;

    /** 状态(0成功 1失败) */
    private Integer status;

    /** 失败信息 */
    private String errorMsg;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
