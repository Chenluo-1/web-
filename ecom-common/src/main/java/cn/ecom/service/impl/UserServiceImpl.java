package cn.ecom.service.impl;

import cn.ecom.domain.dto.PasswordDto;
import cn.ecom.domain.dto.UserDto;
import cn.ecom.domain.pojo.Role;
import cn.ecom.domain.pojo.User;
import cn.ecom.domain.pojo.UserRole;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.RoleMapper;
import cn.ecom.mapper.UserMapper;
import cn.ecom.mapper.UserRoleMapper;
import cn.ecom.service.UserService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.RedisCache;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** 新增用户未填密码时的默认密码 */
    private static final String DEFAULT_PASSWORD = "123456";
    /** 密码最小长度（注册/重置/修改统一口径） */
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final PasswordEncoder passwordEncoder;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RedisCache redisCache;

    public UserServiceImpl(PasswordEncoder passwordEncoder, UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                           RedisCache redisCache) {
        this.passwordEncoder = passwordEncoder;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.redisCache = redisCache;
    }

    @Override
    public Page<User> selectByPage(Integer pageNum, Integer pageSize, User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (user != null) {
            wrapper.like(StringUtils.hasText(user.getUsername()), User::getUsername, user.getUsername());
            wrapper.like(StringUtils.hasText(user.getNickName()), User::getNickName, user.getNickName());
            wrapper.eq(user.getUserType() != null, User::getUserType, user.getUserType());
            wrapper.eq(user.getStatus() != null, User::getStatus, user.getStatus());
        }
        wrapper.orderByAsc(User::getId);
        Page<User> page = page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }

    @Override
    public List<User> selectAll() {
        List<User> list = list();
        list.forEach(u -> u.setPassword(null));
        return list;
    }

    @Override
    public User getUserById(Long id) {
        User user = getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    @Transactional
    public void addUser(UserDto userDto) {
        if (!StringUtils.hasText(userDto.getUsername())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "登录名不能为空");
        }
        Integer count = lambdaQuery().eq(User::getUsername, userDto.getUsername()).count();
        if (count != null && count > 0) {
            throw new BusinessException(AppHttpCodeEnum.USERNAME_EXIST);
        }
        // 清掉占用该登录名的已删用户（逻辑删除的行查询看不到，却仍占着 uk_username）
        getBaseMapper().purgeDeletedByUsername(userDto.getUsername());
        String rawPassword = StringUtils.hasText(userDto.getPassword()) ? userDto.getPassword() : DEFAULT_PASSWORD;
        userDto.setPassword(passwordEncoder.encode(rawPassword));
        userDto.setId(null);
        save(userDto);
        saveUserRoles(userDto.getId(), userDto.getRoleIds());
    }

    @Override
    @Transactional
    public void register(User user) {
        if (!StringUtils.hasText(user.getUsername())
                || !user.getUsername().matches("^[a-zA-Z0-9_]{4,20}$")) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "用户名需为 4-20 位字母、数字或下划线");
        }
        if (!StringUtils.hasText(user.getPassword()) || user.getPassword().length() < 6) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "密码至少 6 位");
        }
        Integer count = lambdaQuery().eq(User::getUsername, user.getUsername()).count();
        if (count != null && count > 0) {
            throw new BusinessException(AppHttpCodeEnum.USERNAME_EXIST);
        }
        // 同 addUser：释放被已删用户占用的登录名
        getBaseMapper().purgeDeletedByUsername(user.getUsername());
        Role memberRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, "member"));
        if (memberRole == null) {
            throw new BusinessException(AppHttpCodeEnum.SYSTEM_ERROR, "消费者角色未初始化");
        }

        User entity = new User();
        entity.setUsername(user.getUsername());
        entity.setNickName(StringUtils.hasText(user.getNickName()) ? user.getNickName() : user.getUsername());
        entity.setPassword(passwordEncoder.encode(user.getPassword()));
        entity.setUserType(1);
        entity.setPhone(user.getPhone());
        entity.setEmail(user.getEmail());
        entity.setStatus(0);
        save(entity);

        UserRole userRole = new UserRole();
        userRole.setUserId(entity.getId());
        userRole.setRoleId(memberRole.getId());
        userRoleMapper.insert(userRole);
    }

    @Override
    @Transactional
    public void updateUser(UserDto userDto) {
        if (userDto.getId() == null) {
            throw new BusinessException(AppHttpCodeEnum.USER_NOT_EXIST);
        }
        // 密码统一走重置/修改接口
        userDto.setPassword(null);
        updateById(userDto);
        saveUserRoles(userDto.getId(), userDto.getRoleIds());
    }

    @Override
    public void resetPassword(PasswordDto passwordDto) {
        User user = getById(passwordDto.getId());
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.USER_NOT_EXIST);
        }
        if (!StringUtils.hasText(passwordDto.getNewPassword())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "新密码不能为空");
        }
        if (passwordDto.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "密码至少 " + MIN_PASSWORD_LENGTH + " 位");
        }
        // 改密后强制该用户重新登录，避免旧会话继续持有旧凭据
        redisCache.deleteObject(RedisCache.LOGIN_USER_KEY + passwordDto.getId());
        updateById(new User()
                .setId(passwordDto.getId())
                .setPassword(passwordEncoder.encode(passwordDto.getNewPassword())));
    }

    @Override
    public void changePassword(PasswordDto passwordDto) {
        Long userId = SecurityUtils.getUserId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(AppHttpCodeEnum.USER_NOT_EXIST);
        }
        String oldPassword = passwordDto.getOldPassword() == null ? "" : passwordDto.getOldPassword();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(AppHttpCodeEnum.OLD_PASSWORD_ERROR);
        }
        if (!StringUtils.hasText(passwordDto.getNewPassword())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "新密码不能为空");
        }
        if (passwordDto.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "密码至少 " + MIN_PASSWORD_LENGTH + " 位");
        }
        redisCache.deleteObject(RedisCache.LOGIN_USER_KEY + userId);
        updateById(new User()
                .setId(userId)
                .setPassword(passwordEncoder.encode(passwordDto.getNewPassword())));
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> ids) {
        removeByIds(ids);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, ids));
        // 账号已删则登录态必须一起失效，否则被删账号的 token 还能用满 24 小时
        for (Long id : ids) {
            redisCache.deleteObject(RedisCache.LOGIN_USER_KEY + id);
        }
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
    }

    /** 重分配用户角色（roleIds 为 null 表示不改动） */
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
        // 角色变了，权限缓存必须失效（否则新权限要等登录态过期才生效）
        redisCache.deleteObject(RedisCache.LOGIN_USER_KEY + userId);
    }
}
