package cn.ecom.domain.vo;

import cn.ecom.domain.pojo.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录返回信息：用户 + token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVo {
    private User user;
    private String token;
}
