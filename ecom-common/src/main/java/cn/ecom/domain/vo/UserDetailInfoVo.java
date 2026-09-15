package cn.ecom.domain.vo;

import cn.ecom.domain.pojo.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * /getInfo 返回：用户信息 + 权限标识集合 + 角色集合
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailInfoVo {
    private User user;
    private List<String> permissions;
    private List<String> roles;
}
