package cn.ecom.controller;

import cn.ecom.domain.vo.SearchResultVo;
import cn.ecom.service.SearchService;
import cn.ecom.utils.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 智能搜索（游客可用）：关键词搜索 / 热词 / 联想
 */
@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** 搜索（同义词扩展 + 容错模糊 + 无结果推荐；记录搜索日志） */
    @GetMapping
    public ResponseResult<SearchResultVo> search(String keyword, Integer pageNum, Integer pageSize) {
        return ResponseResult.okResult(searchService.search(keyword, pageNum, pageSize));
    }

    /** 热门搜索词 */
    @GetMapping("/hotWords")
    public ResponseResult<List<Map<String, Object>>> hotWords(@RequestParam(defaultValue = "10") Integer limit) {
        return ResponseResult.okResult(searchService.hotWords(limit));
    }

    /** 联想词 */
    @GetMapping("/suggest")
    public ResponseResult<List<String>> suggest(String prefix) {
        return ResponseResult.okResult(searchService.suggest(prefix));
    }
}
