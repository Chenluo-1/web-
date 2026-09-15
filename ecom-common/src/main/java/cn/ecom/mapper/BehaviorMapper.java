package cn.ecom.mapper;

import cn.ecom.domain.pojo.Behavior;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

public interface BehaviorMapper extends BaseMapper<Behavior> {

    /** 区间内某类事件数量（漏斗用） */
    @Select("SELECT COUNT(*) FROM ec_user_behavior WHERE del_flag = 0 AND event_type = #{eventType} " +
            "AND create_time >= #{from} AND create_time < #{to}")
    Integer countEvent(@Param("eventType") String eventType, @Param("from") Date from, @Param("to") Date to);

    /** 区间内访客数（去重用户） */
    @Select("SELECT COUNT(DISTINCT user_id) FROM ec_user_behavior WHERE del_flag = 0 AND user_id IS NOT NULL " +
            "AND create_time >= #{from} AND create_time < #{to}")
    Integer countDistinctUser(@Param("from") Date from, @Param("to") Date to);
}
