package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Behavior;
import cn.ecom.mapper.BehaviorMapper;
import cn.ecom.service.BehaviorService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户行为记录（漏斗与推荐的埋点来源）
 */
@Service("behaviorService")
public class BehaviorServiceImpl extends ServiceImpl<BehaviorMapper, Behavior> implements BehaviorService {

    @Override
    public void record(Long userId, String eventType, String targetType, Long targetId) {
        if (!StringUtils.hasText(eventType) || !StringUtils.hasText(targetType) || targetId == null) {
            return;
        }
        save(new Behavior()
                .setUserId(userId)
                .setEventType(eventType)
                .setTargetType(targetType)
                .setTargetId(targetId));
    }
}
