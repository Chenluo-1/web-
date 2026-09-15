package cn.ecom.service.impl;

import cn.ecom.config.DeepSeekClient;
import cn.ecom.domain.pojo.ChatLog;
import cn.ecom.domain.pojo.Knowledge;
import cn.ecom.domain.pojo.Order;
import cn.ecom.domain.pojo.OrderItem;
import cn.ecom.domain.vo.ChatReplyVo;
import cn.ecom.domain.vo.PendingVo;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.ChatLogMapper;
import cn.ecom.mapper.KnowledgeMapper;
import cn.ecom.mapper.OrderItemMapper;
import cn.ecom.mapper.OrderMapper;
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
 * 智能客服（四层分流）：
 * ①知识库规则命中（2-gram 余弦+包含度，≥ 阈值默认 0.85）→ 秒回；
 * ②订单查询意图（按登录用户匹配真实订单，DeepSeek 润色或结构化文本兜底）；
 * ③DeepSeek 大模型兜底；④转人工并进入待补充闭环。
 */
@Service("chatService")
public class ChatServiceImpl extends ServiceImpl<ChatLogMapper, ChatLog> implements ChatService {

    private static final String TRANSFER_REPLY = "抱歉，我暂时无法准确回答这个问题，正在为您转接人工客服～";

    private final KnowledgeMapper knowledgeMapper;
    private final SysConfigService sysConfigService;
    private final DeepSeekClient deepSeekClient;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public ChatServiceImpl(KnowledgeMapper knowledgeMapper, SysConfigService sysConfigService,
                           DeepSeekClient deepSeekClient, OrderMapper orderMapper,
                           OrderItemMapper orderItemMapper) {
        this.knowledgeMapper = knowledgeMapper;
        this.sysConfigService = sysConfigService;
        this.deepSeekClient = deepSeekClient;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
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

        String reply;
        boolean transfer;
        String source;
        String matchedQuestion = null;
        BigDecimal confidence = BigDecimal.valueOf(bestScore).setScale(2, RoundingMode.HALF_UP);
        Long userId = SecurityUtils.getUserId();

        if (best != null && bestScore >= threshold) {
            // 第一层：知识库规则命中
            reply = best.getAnswer();
            transfer = false;
            source = "rule";
            matchedQuestion = best.getQuestion();
        } else if (isTransferRequest(question)) {
            // 明确要求转人工：真实登记转接（不再让大模型"假装"转接）
            reply = "好的，已为您登记转接人工客服，客服主管会尽快跟进，请稍后～";
            transfer = true;
            source = "transfer";
        } else if (isOrderQuery(question)) {
            // 第二层：订单查询（按登录用户匹配真实订单，固定精简格式直出，不经过大模型）
            Integer nth = parseOrderPosition(question);
            String orderData = nth != null
                    ? buildNthOrder(userId, nth)
                    : buildOrderData(userId, isLogisticsQuery(question));
            reply = StringUtils.hasText(orderData) ? orderData.trim() : "您暂无订单记录，去商城逛逛吧～";
            transfer = false;
            source = "order";
            confidence = null;
        } else {
            // 第三层：大模型兜底（开关关闭/未配密钥/调用失败 → null）
            String llmReply = llmReply(question);
            if (StringUtils.hasText(llmReply)) {
                reply = llmReply;
                transfer = false;
                source = "llm";
                confidence = null;
            } else {
                // 第四层：转人工（进入待补充闭环）
                reply = TRANSFER_REPLY;
                transfer = true;
                source = "transfer";
            }
        }

        save(new ChatLog()
                .setUserId(userId)
                .setQuestion(question)
                .setAnswer(reply)
                .setConfidence(confidence)
                .setIsTransfer(transfer ? 1 : 0)
                .setSource(source));

        ChatReplyVo vo = new ChatReplyVo();
        vo.setReply(reply);
        vo.setConfidence(confidence);
        vo.setTransfer(transfer);
        vo.setMatchedQuestion(matchedQuestion);
        vo.setSource(source);
        return vo;
    }

    /** 大模型开关（ec_sys_config.chat_llm_enabled，默认开） */
    private boolean llmEnabled() {
        return "1".equals(sysConfigService.getString("chat_llm_enabled", "1"));
    }

    /** 大模型兜底回复；开关关闭或调用失败时返回 null */
    private String llmReply(String question) {
        if (!llmEnabled()) {
            return null;
        }
        return deepSeekClient.chat(buildLlmSystemPrompt(), question);
    }

    // ---------- 订单查询层 ----------

    private static final String[] ORDER_INTENT_KEYS = {
            "我的订单", "查订单", "查询订单", "订单状态", "订单详情", "我的单子", "我买的东西",
            "购买记录", "历史订单", "我买了什么", "买过什么", "订单编号", "订单号", "单号",
            "账户下的订单", "下单了", "下单记录", "下单的东西", "买的什么", "哪些东西", "什么时候能到",
            "物流", "快递", "包裹", "到哪了", "到哪儿", "发货了吗", "发货了嘛", "运单", "什么时候到"};

    /** 政策类问法排除：这些问的是规则不是个人订单（问法压测补充） */
    private static final String[] ORDER_EXCLUDE_KEYS = {
            "能放", "放驿站", "送货上门", "指定快递", "发什么快递", "能加急", "怎么取消", "如何取消"};

    private boolean isOrderQuery(String question) {
        for (String key : ORDER_EXCLUDE_KEYS) {
            if (question.contains(key)) {
                return false;
            }
        }
        for (String key : ORDER_INTENT_KEYS) {
            if (question.contains(key)) {
                return true;
            }
        }
        // 位置指代追问（我要最近的一单 / 第2单呢 / 第一个订单）
        if (parseOrderPosition(question) != null
                && (question.contains("单") || question.contains("笔") || question.contains("订单"))) {
            return true;
        }
        // 组合规则：出现「我/查/看」+「订单/下单/购买」视为查自己的订单
        boolean mine = question.contains("我") || question.contains("查") || question.contains("看");
        boolean aboutOrder = question.contains("订单") || question.contains("下单")
                || question.contains("购买") || question.contains("买过");
        return mine && aboutOrder;
    }

    // ---------- 订单位置指代（追问「最近一单/第 N 单」） ----------

    private static final java.util.Map<Character, Integer> CN_NUM = new java.util.HashMap<>();

    static {
        String cn = "一二两三四五六七八九";
        for (int i = 0; i < cn.length(); i++) {
            CN_NUM.put(cn.charAt(i), i + 1);
        }
        CN_NUM.put('十', 10);
        for (char c = '1'; c <= '9'; c++) {
            CN_NUM.put(c, c - '0');
        }
    }

    private static final java.util.regex.Pattern POS_NTH = java.util.regex.Pattern.compile(
            "第([一二两三四五六七八九十]{1,3}|\\d{1,2})|([一二两三四五六七八九1-9])\\s*[单笔]");

    /** 位置指代 → 第几笔（列表按最新排序，最近/最新/最后/上一 视为第 1 笔）；无指代返回 null */
    private Integer parseOrderPosition(String question) {
        if (question.contains("最近") || question.contains("最新")
                || question.contains("最后") || question.contains("上一")) {
            return 1;
        }
        java.util.regex.Matcher m = POS_NTH.matcher(question);
        if (m.find()) {
            if (m.group(1) != null) {
                return cnNum(m.group(1));
            }
            return CN_NUM.get(m.group(2).charAt(0));
        }
        return null;
    }

    /** 中文/阿拉伯数字解析：十=10、十一=11、二十=20、二十一=21、\"3\"=3 */
    private Integer cnNum(String s) {
        if (s.matches("\\d+")) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (s.length() == 1) {
            return CN_NUM.get(s.charAt(0));
        }
        int idx = s.indexOf('十');
        if (idx == 0) {
            return 10 + CN_NUM.getOrDefault(s.charAt(1), 0);
        }
        int tens = CN_NUM.getOrDefault(s.charAt(0), 0) * 10;
        return s.length() == 2 ? tens : tens + CN_NUM.getOrDefault(s.charAt(2), 0);
    }

    /** 追问单笔订单：第 nth 笔（1=最近一笔）；不足时提示并附全列表 */
    private String buildNthOrder(Long userId, int nth) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getId)
                .last("LIMIT " + nth));
        if (orders.isEmpty()) {
            return "您暂无订单记录，去商城逛逛吧～";
        }
        if (orders.size() < nth) {
            return "您目前只有 " + orders.size() + " 笔订单（没有第 " + nth + " 笔哦）～以下是您最近的订单：\n"
                    + buildOrderData(userId, false);
        }
        return buildOrderLine(orders.get(nth - 1));
    }

    /** 明确要求转人工的问法（直接进入转人工，不让大模型"假装"转接） */
    private static final String[] TRANSFER_INTENT_KEYS = {
            "转人工", "人工客服", "找人工", "要人工", "真人客服", "转接人工"};

    private boolean isTransferRequest(String question) {
        for (String key : TRANSFER_INTENT_KEYS) {
            if (question.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static final String[] LOGISTICS_INTENT_KEYS = {
            "物流", "快递", "包裹", "到哪了", "到哪儿", "运单", "什么时候到"};

    /** 物流类问法：优先取有物流记录的订单（运输中/已完成） */
    private boolean isLogisticsQuery(String question) {
        for (String key : LOGISTICS_INTENT_KEYS) {
            if (question.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static final String[] STATUS_NAMES = {"待支付", "待发货", "运输中", "已完成", "已取消", "退款中", "已退款"};

    /** 按登录用户匹配真实订单，格式化为文本块；无订单返回空串 */
    private String buildOrderData(Long userId, boolean logisticsFocus) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId);
        if (logisticsFocus) {
            wrapper.in(Order::getStatus, 2, 3);
        }
        wrapper.orderByDesc(Order::getId).last("LIMIT " + (logisticsFocus ? 3 : 5));
        List<Order> orders = orderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            // 物流视角无记录时退回全量最近订单
            return logisticsFocus ? buildOrderData(userId, false) : "";
        }
        StringBuilder sb = new StringBuilder();
        for (Order order : orders) {
            sb.append(buildOrderLine(order)).append('\n');
        }
        return sb.toString();
    }

    /** 单笔订单行：订单：单号 状态 ¥金额 商品 */
    private String buildOrderLine(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        StringBuilder sb = new StringBuilder();
        sb.append("订单：").append(order.getOrderNo())
                .append(' ').append(statusName(order.getStatus()))
                .append(" ¥").append(order.getPayAmount());
        if (!items.isEmpty()) {
            sb.append(' ').append(items.get(0).getProductName());
            if (items.size() > 1) {
                sb.append(" 等").append(items.size()).append("件");
            }
        }
        return sb.toString();
    }

    private String statusName(Integer status) {
        if (status == null || status < 0 || status >= STATUS_NAMES.length) {
            return "未知";
        }
        return STATUS_NAMES[status];
    }

    /** 组装提示词：电商客服角色约束 + 知识库条目作为参考资料（轻量 RAG） */
    private String buildLlmSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是电商店铺「电商智能系统」的在线客服助手。要求：")
                .append("只用简洁友好的中文回答；围绕商品、订单、支付、物流、退换货、优惠券等售前售后问题作答；")
                .append("店铺没有的信息不要编造，不确定时礼貌说明并建议转人工客服。\n")
                .append("以下为本店知识库资料，与用户问题相关时优先按资料回答：\n");
        for (Knowledge knowledge : activeKnowledges()) {
            sb.append("【问】").append(knowledge.getQuestion()).append('\n')
                    .append("【答】").append(knowledge.getAnswer()).append('\n');
        }
        return sb.toString();
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
            // "转人工"这类请求本身不算需要补充知识的问题
            if (isTransferRequest(question)) {
                continue;
            }
            double best = 0;
            for (Knowledge knowledge : knowledges) {
                best = Math.max(best, matchScore(question, knowledge));
            }
            // 已被新知识覆盖的问题自动移出待补充
            if (best >= threshold) {
                continue;
            }
            Object lastTimeVal = null;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey().replace("_", "").toLowerCase();
                if (key.contains("lasttime")) {
                    lastTimeVal = entry.getValue();
                    break;
                }
            }
            String lastTime = null;
            if (lastTimeVal instanceof Date) {
                lastTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) lastTimeVal);
            } else if (lastTimeVal != null) {
                lastTime = String.valueOf(lastTimeVal);
            }
            result.add(new PendingVo(question,
                    row.get("cnt") == null ? 0L : ((Number) row.get("cnt")).longValue(),
                    lastTime));
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
