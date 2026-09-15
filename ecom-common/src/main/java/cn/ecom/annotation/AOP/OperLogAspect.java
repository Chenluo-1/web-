package cn.ecom.annotation.AOP;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.pojo.LoginUser;
import cn.ecom.domain.pojo.SysOperLog;
import cn.ecom.mapper.SysOperLogMapper;
import cn.ecom.utils.SecurityUtils;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 写操作日志切面：@OperLog 标注的方法自动记录到 sys_oper_log
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    private final SysOperLogMapper operLogMapper;

    public OperLogAspect(SysOperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        SysOperLog logRecord = new SysOperLog()
                .setModule(operLog.module())
                .setOperType(operLog.type())
                .setMethod(pjp.getSignature().toShortString());

        try {
            logRecord.setParams(maskPassword(JSON.toJSONString(pjp.getArgs())));
        } catch (Exception ignored) {
        }

        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null && loginUser.getUser() != null) {
                logRecord.setOperatorId(loginUser.getUser().getId());
                logRecord.setOperatorName(loginUser.getUser().getUsername());
            }
        } catch (Exception ignored) {
        }

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                logRecord.setIp(request.getRemoteAddr());
            }
        } catch (Exception ignored) {
        }

        Object result;
        try {
            result = pjp.proceed();
            logRecord.setStatus(0);
        } catch (Throwable t) {
            logRecord.setStatus(1);
            String msg = t.getMessage();
            if (msg != null && msg.length() > 490) {
                msg = msg.substring(0, 490);
            }
            logRecord.setErrorMsg(msg);
            throw t;
        } finally {
            try {
                operLogMapper.insert(logRecord);
            } catch (Exception e) {
                log.warn("操作日志记录失败：{}", e.getMessage());
            }
        }
        return result;
    }

    /** 请求参数中的密码脱敏 */
    private String maskPassword(String json) {
        if (json == null) {
            return null;
        }
        return json.replaceAll("(\"(?:password|oldPassword|newPassword)\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
    }
}
