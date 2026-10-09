package cn.ecom.mapper;

import cn.ecom.domain.pojo.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    /** 查询用户的角色 key 集合 */
    List<String> selectRoleKeyByUserId(@Param("userId") Long userId);

    /** 查询用户的角色 id 集合 */
    List<Long> selectRoleIdByUserId(@Param("userId") Long userId);

    /** 物理清理占用权限字符的已删角色（同 sys_user.uk_username 的唯一索引问题） */
    @Delete("DELETE FROM sys_role WHERE role_key = #{roleKey} AND del_flag = 1")
    int purgeDeletedByRoleKey(@Param("roleKey") String roleKey);
}
