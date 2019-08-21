package cn.ocoop.framework.safe.ex;

import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import cn.ocoop.framework.safe.ex.authz.InvalidCaptchaException;
import cn.ocoop.framework.safe.ex.authz.LackPermissionException;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ExceptionAdviceHandler {

    @ExceptionHandler(AuthorizingException.class)
    public Map<String, String> authExceptionHandler(HttpServletRequest request, HttpServletResponse response, AuthorizingException ex) {
        int status = 401; //未登录
        if (ex instanceof LackPermissionException) {
            status = 444;//无权限
        } else if (ex instanceof InvalidCaptchaException) {
            status = 455;//验证码错误
        }
        response.setStatus(status);

        Map<String, String> errorMsg = Maps.newHashMap();
        errorMsg.put("title", "不满足访问条件");
        errorMsg.put("exception", ClassUtils.getShortName(ex.getClass()));
        errorMsg.put("message", ex.getMessage());
        return errorMsg;
    }
}
