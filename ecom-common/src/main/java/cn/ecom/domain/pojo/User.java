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
 * 用户表实体（运营+消费者单表，user_type 区分）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("sys_user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名 */
    private String username;

    /** 昵称/姓名 */
    private String nickName;

    /** 密码（BCrypt） */
    private String password;

    /** 用户类型(0运营 1消费者) */
    private Integer userType;

    private String phone;

    private String email;

    private String avatar;

    /** 状态(0正常 1停用) */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
