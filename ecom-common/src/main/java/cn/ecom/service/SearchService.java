package cn.ecom.service;

import cn.ecom.domain.vo.SearchResultVo;

import java.util.List;
import java.util.Map;

public interface SearchService {

    /** 搜索：同义词扩展 → 标题匹配 → 容错模糊 → 无结果推荐；并记录搜索日志 */
    SearchResultVo search(String keyword, Integer pageNum, Integer pageSize);

    /** 热门搜索词（近 30 天） */
    List<Map<String, Object>> hotWords(int limit);

    /** 联想词 */
    List<String> suggest(String prefix);
}
