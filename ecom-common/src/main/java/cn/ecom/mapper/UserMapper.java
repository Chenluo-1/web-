package cn.ecom.mapper;

import cn.ecom.domain.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {

    /**
     * 物理清理占用登录名的已删用户。
     * sys_user 上有唯一索引 uk_username，而逻辑删除的旧行不会被 MyBatis-Plus 的
     * 查询条件看到——于是「删掉 alice 再新建 alice」会通过重名校验却撞唯一索引。
     * 新建前先清掉同名的已删行（它已在业务上被删除），保证删除后可以重建同名账号。
     */
    @Delete("DELETE FROM sys_user WHERE username = #{username} AND del_flag = 1")
    int purgeDeletedByUsername(@Param("username") String username);
}
