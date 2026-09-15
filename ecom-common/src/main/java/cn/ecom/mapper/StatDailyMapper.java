package cn.ecom.mapper;

import cn.ecom.domain.pojo.StatDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface StatDailyMapper extends BaseMapper<StatDaily> {

    /** T+1 重算：物理删除指定日期的汇总行 */
    @Delete("DELETE FROM ec_stat_daily WHERE stat_date = #{statDate}")
    int deleteByDate(@Param("statDate") Date statDate);
}
