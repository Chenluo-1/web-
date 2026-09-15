package cn.ecom.controller;

import cn.ecom.service.BehaviorService;
import cn.ecom.utils.ResponseResult;
import cn.ecom.utils.SecurityUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户行为上报（曝光/点击；登录可选，游客 userId 为空）
 */
@RestController
@RequestMapping("/behavior")
public class BehaviorController {

    private final BehaviorService behaviorService;

    public BehaviorController(BehaviorService behaviorService) {
        this.behaviorService = behaviorService;
    }

    @PostMapping("/report")
    public ResponseResult<Void> report(@RequestBody Map<String, Object> body) {
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception ignored) {
            // 游客
        }
        String eventType = body.get("eventType") == null ? null : body.get("eventType").toString();
        String targetType = body.get("targetType") == null ? null : body.get("targetType").toString();
        Long targetId = body.get("targetId") == null ? null : Long.valueOf(body.get("targetId").toString());
        behaviorService.record(userId, eventType, targetType, targetId);
        return ResponseResult.okResult();
    }
}
