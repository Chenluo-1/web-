package cn.ecom.domain.dto;

import cn.ecom.domain.pojo.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户新增/修改 DTO：用户字段 + 角色 id 集合
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDto extends User {
    private List<Long> roleIds;
}
