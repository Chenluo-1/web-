package cn.ecom.controller;

import cn.ecom.utils.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    /** 前后端连通验证用，P1 之后保留（运维健康检查） */
    @GetMapping("/health")
    public ResponseResult<String> health() {
        return ResponseResult.okResult("ok");
    }
}
