package cn.ecom.service;

import cn.ecom.domain.dto.PasswordDto;
import cn.ecom.domain.dto.UserDto;
import cn.ecom.domain.pojo.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserService extends IService<User> {

    /** 分页查询（支持 username/nickName 模糊、userType/status 筛选） */
    Page<User> selectByPage(Integer pageNum, Integer pageSize, User user);

    List<User> selectAll();

    User getUserById(Long id);

    /** 新增用户（默认密码 123456），并分配角色 */
    void addUser(UserDto userDto);

    /** 消费者自助注册（固定 user_type=1，自动绑定 member 角色） */
    void register(User user);

    /** 修改用户（不含密码），并重分配角色 */
    void updateUser(UserDto userDto);

    /** 启用/禁用账号（0 正常 1 停用；停用同时失效其登录态） */
    void updateStatus(Long id, Integer status);

    /** 管理员重置密码 */
    void resetPassword(PasswordDto passwordDto);

    /** 本人修改密码（校验旧密码） */
    void changePassword(PasswordDto passwordDto);

    /** 删除用户（逻辑删除，同时清角色关联） */
    void deleteUsers(List<Long> ids);

    /** 用户的角色 id 集合 */
    List<Long> getRoleIdsByUserId(Long userId);
}
