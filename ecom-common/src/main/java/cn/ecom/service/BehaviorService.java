package cn.ecom.service;

import cn.ecom.domain.pojo.Behavior;
import com.baomidou.mybatisplus.extension.service.IService;

public interface BehaviorService extends IService<Behavior> {

    /** 记录行为（expose/click 由前端上报，cart 由后端加购时记录；userId 可为空=游客） */
    void record(Long userId, String eventType, String targetType, Long targetId);
}
