package cn.ecom.service;

import cn.ecom.domain.pojo.ChatLog;
import cn.ecom.domain.vo.ChatReplyVo;
import cn.ecom.domain.vo.PendingVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ChatService extends IService<ChatLog> {

    /** 问答：bigram 余弦相似度匹配知识库，置信度 ≥ 阈值自动回复，低于阈值转人工并记日志 */
    ChatReplyVo ask(String question);

    /** 会话记录分页（可按是否转人工筛选） */
    Page<ChatLog> selectLogsByPage(Integer pageNum, Integer pageSize, Integer isTransfer);

    /** 待补充问题（转人工且当前仍匹配不到知识的，去重） */
    List<PendingVo> listPending();
}
