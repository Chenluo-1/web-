package cn.ecom.service.impl;

import cn.ecom.domain.pojo.ChatLog;
import cn.ecom.domain.pojo.Knowledge;
import cn.ecom.domain.vo.ChatReplyVo;
import cn.ecom.domain.vo.PendingVo;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.ChatLogMapper;
import cn.ecom.mapper.KnowledgeMapper;
import cn.ecom.service.ChatService;
import cn.ecom.service.SysConfigService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.SecurityUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能客服（规则版）：
 * 对问题做字符级 2-gram 归一化后计算余弦相似度（对中文短句无需外部分词库，容忍少量错字与语序差异），
 * 取标准问法与相似问法的最高分作为置信度；≥ 阈值（ec_sys_config，默认 0.85）自动回复，否则转人工。
 */
@Service("chatService")
public class ChatServiceImpl extends ServiceImpl<ChatLogMapper, ChatLog> implements ChatService {

    private static final String TRANSFER_REPLY = "抱歉，我暂时无法准确回答这个问题，正在为您转接人工客服～";

    private final KnowledgeMapper knowledgeMapper;
    private final SysConfigService sysConfigService;

    public ChatServiceImpl(KnowledgeMapper knowledgeMapper, SysConfigService sysConfigService) {
        this.knowledgeMapper = knowledgeMapper;
        this.sysConfigService = sysConfigService;
    }

    @Override
    public ChatReplyVo ask(String question) {
        if (!StringUtils.hasText(question)) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "请输入问题");
        }
        double threshold = threshold();
        Knowledge best = null;
        double bestScore = 0;
        for (Knowledge knowledge : activeKnowledges()) {
            double score = matchScore(question, knowledge);
            if (score > bestScore) {
                bestScore = score;
                best = knowledge;
            }
        }
        boolean transfer = best == null || bestScore < threshold;
        BigDecimal confidence = BigDecimal.valueOf(bestScore).setScale(2, RoundingMode.HALF_UP);

        Long userId = SecurityUtils.getUserId();
        save(new ChatLog()
                .setUserId(userId)
                .setQuestion(question)
                .setAnswer(transfer ? TRANSFER_REPLY : best.getAnswer())
                .setConfidence(confidence)
                .setIsTransfer(transfer ? 1 : 0));

        ChatReplyVo vo = new ChatReplyVo();
        vo.setReply(transfer ? TRANSFER_REPLY : best.getAnswer());
        vo.setConfidence(confidence);
        vo.setTransfer(transfer);
        vo.setMatchedQuestion(transfer || best == null ? null : best.getQuestion());
        return vo;
    }

    @Override
    public Page<ChatLog> selectLogsByPage(Integer pageNum, Integer pageSize, Integer isTransfer) {
        return page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize),
                new LambdaQueryWrapper<ChatLog>()
                        .eq(isTransfer != null, ChatLog::getIsTransfer, isTransfer)
                        .orderByDesc(ChatLog::getId));
    }

    @Override
    public List<PendingVo> listPending() {
        double threshold = threshold();
        List<Knowledge> knowledges = activeKnowledges();
        List<PendingVo> result = new ArrayList<>();
        for (Map<String, Object> row : getBaseMapper().selectPendingQuestions()) {
            String question = String.valueOf(row.get("question"));
            double best = 0;
            for (Knowledge knowledge : knowledges) {
                best = Math.max(best, matchScore(question, knowledge));
            }
            // 已被新知识覆盖的问题自动移出待补充
            if (best >= threshold) {
                continue;
            }
            Object lastTime = row.get("lastTime");
            if (lastTime == null) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getKey().toLowerCase().contains("lasttime")) {
                        lastTime = entry.getValue();
                        break;
                    }
                }
            }
            result.add(new PendingVo(question,
                    row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue(),
                    lastTime instanceof Date ? (Date) lastTime : null));
        }
        return result;
    }

    private double threshold() {
        String value = sysConfigService.getString("chat_confidence_threshold", "0.85");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.85;
        }
    }

    private List<Knowledge> activeKnowledges() {
        return knowledgeMapper.selectList(new LambdaQueryWrapper<Knowledge>().eq(Knowledge::getStatus, 1));
    }

    /** 相似度 = max(标准问法, 各相似问法)，单项取 max(2-gram 余弦, 知识问法包含度) */
    private double matchScore(String query, Knowledge knowledge) {
        double score = similarity(query, knowledge.getQuestion());
        if (StringUtils.hasText(knowledge.getSimilarQuestions())) {
            try {
                JSONArray similar = JSON.parseArray(knowledge.getSimilarQuestions());
                for (int i = 0; i < similar.size(); i++) {
                    score = Math.max(score, similarity(query, similar.getString(i)));
                }
            } catch (Exception ignored) {
                // 相似问法 JSON 异常时忽略，仅用标准问法
            }
        }
        return score;
    }

    /**
     * 单项相似度：余弦（对称，容忍错字）与包含度（知识问法 2-gram 被查询全覆盖的比例，
     * 容忍多久发货呀请问这类加料问句）取最大。
     */
    private double similarity(String query, String known) {
        Map<String, Integer> queryGrams = bigrams(query);
        Map<String, Integer> knownGrams = bigrams(known);
        if (knownGrams.isEmpty()) {
            return 0;
        }
        int common = 0;
        for (String gram : knownGrams.keySet()) {
            if (queryGrams.containsKey(gram)) {
                common++;
            }
        }
        double containment = (double) common / knownGrams.size();
        return Math.max(cosine(queryGrams, knownGrams), containment);
    }

    /** 归一化：去标点空格、统一小写 */
    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("[^\\p{IsHan}\\p{Alnum}]", "").toLowerCase();
    }

    /** 字符级 2-gram 词频表（中文免分词的轻量方案） */
    private Map<String, Integer> bigrams(String text) {
        String n = normalize(text);
        Map<String, Integer> map = new HashMap<>();
        if (n.isEmpty()) {
            return map;
        }
        if (n.length() == 1) {
            map.put(n, 1);
            return map;
        }
        for (int i = 0; i + 1 < n.length(); i++) {
            map.merge(n.substring(i, i + 2), 1, Integer::sum);
        }
        return map;
    }

    private double cosine(Map<String, Integer> a, Map<String, Integer> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (Map.Entry<String, Integer> entry : a.entrySet()) {
            normA += (double) entry.getValue() * entry.getValue();
            Integer bv = b.get(entry.getKey());
            if (bv != null) {
                dot += (double) entry.getValue() * bv;
            }
        }
        for (Integer v : b.values()) {
            normB += (double) v * v;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
