package cn.ecom.service;

import cn.ecom.domain.pojo.Knowledge;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface KnowledgeService extends IService<Knowledge> {

    Page<Knowledge> selectByPage(Integer pageNum, Integer pageSize, String question, Integer status);

    void addKnowledge(Knowledge knowledge);

    void updateKnowledge(Knowledge knowledge);

    void deleteKnowledges(java.util.List<Long> ids);
}
