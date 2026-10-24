package cn.ecom.controller;

import cn.ecom.domain.pojo.ChatLog;
import cn.ecom.domain.vo.ChatReplyVo;
import cn.ecom.domain.vo.PendingVo;
import cn.ecom.service.ChatService;
import cn.ecom.utils.ResponseResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 智能客服：问答（登录用户）/ 会话记录与待补充（客服主管）
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 提问（置信度 ≥ 阈值自动回复，否则转人工） */
    @PostMapping("/ask")
    public ResponseResult<ChatReplyVo> ask(@RequestBody Map<String, Object> body) {
        String question = body.get("question") == null ? null : body.get("question").toString();
        return ResponseResult.okResult(chatService.ask(question));
    }

    /** 会话记录 */
    @GetMapping("/logs")
    @PreAuthorize("@ss.hasPermission('chat:manage')")
    public ResponseResult<Page<ChatLog>> logs(Integer pageNum, Integer pageSize, Integer isTransfer) {
        return ResponseResult.okResult(chatService.selectLogsByPage(pageNum, pageSize, isTransfer));
    }

    /** 待补充问题（转人工且仍匹配不到知识的） */
    @GetMapping("/pending")
    @PreAuthorize("@ss.hasPermission('chat:manage')")
    public ResponseResult<List<PendingVo>> pending() {
        return ResponseResult.okResult(chatService.listPending());
    }
}
