package cn.ecom.mapper;

import cn.ecom.domain.pojo.SearchLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SearchLogMapper extends BaseMapper<SearchLog> {

    /** 热门搜索词（近 30 天 top N） */
    @Select("SELECT keyword, COUNT(*) AS cnt FROM ec_search_log " +
            "WHERE del_flag = 0 AND create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY keyword ORDER BY cnt DESC, MAX(create_time) DESC LIMIT #{limit}")
    List<Map<String, Object>> selectHotWords(int limit);

    /** 联想词（前缀/包含匹配去重） */
    @Select("SELECT DISTINCT keyword FROM ec_search_log " +
            "WHERE del_flag = 0 AND keyword LIKE CONCAT('%', #{prefix}, '%') LIMIT 8")
    List<String> selectSuggest(String prefix);
}
