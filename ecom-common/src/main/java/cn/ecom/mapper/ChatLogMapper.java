package cn.ecom.mapper;

import cn.ecom.domain.pojo.ChatLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ChatLogMapper extends BaseMapper<ChatLog> {

    /** 待补充问题：转人工的原始问题（去重 + 次数 + 最近时间），由服务层与知识库再比对过滤 */
    @Select("SELECT question, COUNT(*) AS cnt, MAX(create_time) AS lastTime FROM ec_chat_log " +
            "WHERE is_transfer = 1 AND del_flag = 0 GROUP BY question ORDER BY lastTime DESC LIMIT 100")
    List<Map<String, Object>> selectPendingQuestions();
}
