package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Knowledge;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.KnowledgeMapper;
import cn.ecom.service.KnowledgeService;
import cn.ecom.utils.AppHttpCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 知识库管理（客服主管）
 */
@Service("knowledgeService")
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> implements KnowledgeService {

    @Override
    public Page<Knowledge> selectByPage(Integer pageNum, Integer pageSize, String question, Integer status) {
        return page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize),
                new LambdaQueryWrapper<Knowledge>()
                        .like(StringUtils.hasText(question), Knowledge::getQuestion, question)
                        .eq(status != null, Knowledge::getStatus, status)
                        .orderByDesc(Knowledge::getId));
    }

    @Override
    public void addKnowledge(Knowledge knowledge) {
        validate(knowledge);
        knowledge.setId(null);
        if (knowledge.getStatus() == null) {
            knowledge.setStatus(1);
        }
        save(knowledge);
    }

    @Override
    public void updateKnowledge(Knowledge knowledge) {
        if (knowledge.getId() == null) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "缺少知识 id");
        }
        validate(knowledge);
        updateById(knowledge);
    }

    @Override
    public void deleteKnowledges(List<Long> ids) {
        removeByIds(ids);
    }

    private void validate(Knowledge knowledge) {
        if (!StringUtils.hasText(knowledge.getQuestion())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "标准问法不能为空");
        }
        if (!StringUtils.hasText(knowledge.getAnswer())) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "标准答案不能为空");
        }
    }
}
