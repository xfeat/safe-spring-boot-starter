package cn.ocoop.framework.safe.ex;

import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import cn.ocoop.framework.safe.ex.authz.InvalidCaptchaException;
import cn.ocoop.framework.safe.ex.authz.InvalidSessionStateException;
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
        int status = 401; //unauthenticated
        if (ex instanceof LackPermissionException) {
            status = 444;//no permission
        } else if (ex instanceof InvalidCaptchaException) {
            status = 455;//invalid captcha
        } else if (ex instanceof InvalidSessionStateException) {
            status = 466;//invalid session state
        }
        response.setStatus(status);

        Map<String, String> errorMsg = Maps.newHashMap();
        errorMsg.put("title", "unsatisfied access condition");
        errorMsg.put("exception", ClassUtils.getShortName(ex.getClass()));
        errorMsg.put("message", ex.getMessage());
        return errorMsg;
    }
}
