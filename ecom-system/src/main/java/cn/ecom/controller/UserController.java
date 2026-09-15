package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.dto.PasswordDto;
import cn.ecom.domain.dto.UserDto;
import cn.ecom.domain.pojo.User;
import cn.ecom.service.UserService;
import cn.ecom.utils.ResponseResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理（/sys/user 标准 CRUD）
 */
@RestController
@RequestMapping("/sys/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 分页查询 */
    @GetMapping
    @PreAuthorize("@ps.hasPermission('sys:user')")
    public ResponseResult<Page<User>> selectByPage(Integer pageNum, Integer pageSize, User user) {
        return ResponseResult.okResult(userService.selectByPage(pageNum, pageSize, user));
    }

    /** 查询全部 */
    @GetMapping("/getAll")
    public ResponseResult<List<User>> selectAll() {
        return ResponseResult.okResult(userService.selectAll());
    }

    /** 按 id 查询 */
    @GetMapping("/{id}")
    public ResponseResult<User> selectOne(@PathVariable Long id) {
        return ResponseResult.okResult(userService.getUserById(id));
    }

    /** 查询用户的角色 id 集合（编辑回显用） */
    @GetMapping("/{id}/roleIds")
    public ResponseResult<List<Long>> getRoleIds(@PathVariable Long id) {
        return ResponseResult.okResult(userService.getRoleIdsByUserId(id));
    }

    /** 新增 */
    @OperLog(module = "用户管理", type = "新增")
    @PreAuthorize("@ps.hasPermission('sys:user:add')")
    @PostMapping
    public ResponseResult<Void> insert(@RequestBody UserDto userDto) {
        userService.addUser(userDto);
        return ResponseResult.okResult();
    }

    /** 修改 */
    @OperLog(module = "用户管理", type = "修改")
    @PreAuthorize("@ps.hasPermission('sys:user:edit')")
    @PutMapping
    public ResponseResult<Void> update(@RequestBody UserDto userDto) {
        userService.updateUser(userDto);
        return ResponseResult.okResult();
    }

    /** 删除（支持批量，路径用逗号分隔） */
    @OperLog(module = "用户管理", type = "删除")
    @PreAuthorize("@ps.hasPermission('sys:user:del')")
    @DeleteMapping("/{ids}")
    public ResponseResult<Void> delete(@PathVariable("ids") List<Long> ids) {
        userService.deleteUsers(ids);
        return ResponseResult.okResult();
    }

    /** 管理员重置密码 */
    @OperLog(module = "用户管理", type = "重置密码")
    @PreAuthorize("@ps.hasPermission('sys:user:respwd')")
    @PutMapping("/respwd")
    public ResponseResult<Void> resetPassword(@RequestBody PasswordDto passwordDto) {
        userService.resetPassword(passwordDto);
        return ResponseResult.okResult();
    }

    /** 本人修改密码 */
    @OperLog(module = "个人中心", type = "修改密码")
    @PutMapping("/password")
    public ResponseResult<Void> changePassword(@RequestBody PasswordDto passwordDto) {
        userService.changePassword(passwordDto);
        return ResponseResult.okResult();
    }
}
