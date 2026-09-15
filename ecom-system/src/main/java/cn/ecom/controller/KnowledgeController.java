package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.pojo.Knowledge;
import cn.ecom.service.KnowledgeService;
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
 * 知识库标准 CRUD（客服主管）
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    @PreAuthorize("@ps.hasPermission('chat:manage')")
    public ResponseResult<Page<Knowledge>> selectByPage(Integer pageNum, Integer pageSize,
                                                        String question, Integer status) {
        return ResponseResult.okResult(knowledgeService.selectByPage(pageNum, pageSize, question, status));
    }

    @OperLog(module = "知识库", type = "新增")
    @PreAuthorize("@ps.hasPermission('knowledge:add')")
    @PostMapping
    public ResponseResult<Void> insert(@RequestBody Knowledge knowledge) {
        knowledgeService.addKnowledge(knowledge);
        return ResponseResult.okResult();
    }

    @OperLog(module = "知识库", type = "修改")
    @PreAuthorize("@ps.hasPermission('knowledge:edit')")
    @PutMapping
    public ResponseResult<Void> update(@RequestBody Knowledge knowledge) {
        knowledgeService.updateKnowledge(knowledge);
        return ResponseResult.okResult();
    }

    @OperLog(module = "知识库", type = "删除")
    @PreAuthorize("@ps.hasPermission('knowledge:del')")
    @DeleteMapping("/{ids}")
    public ResponseResult<Void> delete(@PathVariable("ids") List<Long> ids) {
        knowledgeService.deleteKnowledges(ids);
        return ResponseResult.okResult();
    }
}
