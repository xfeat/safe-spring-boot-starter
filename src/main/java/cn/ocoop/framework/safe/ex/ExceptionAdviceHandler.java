package cn.ocoop.framework.safe.ex;

import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import cn.ocoop.framework.safe.ex.authz.InvalidCaptchaException;
import cn.ocoop.framework.safe.ex.authz.InvalidSessionStateException;
import cn.ocoop.framework.safe.ex.authz.LackPermissionException;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import java.util.Map;

@Slf4j
public class ExceptionAdviceHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AuthorizingException.class)
    public ResponseEntity<Object> authExceptionHandler(WebRequest request, AuthorizingException ex) {
        log.error("error", ex);

        int status = 401; //unauthenticated
        if (ex instanceof LackPermissionException) {
            status = 444;//no permission
        } else if (ex instanceof InvalidCaptchaException) {
            status = 455;//invalid captcha
        } else if (ex instanceof InvalidSessionStateException) {
            status = 466;//invalid session state
        }

        return handleExceptionInternal(ex, errorWrap(status, "unsatisfied access condition", ex.getMessage()), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }

        if (body == null) {
            body = errorWrap(status.value(), status.name(), ex.getLocalizedMessage());
        }

        return ResponseEntity.status(status.value()).headers(headers).body(body);
    }

    protected Map<String, Object> errorWrap(int status, String title, String message) {
        Map<String, Object> errorMsg = Maps.newHashMap();
        errorMsg.put("title", title);
        errorMsg.put("code", status);
        errorMsg.put("message", message);
        return errorMsg;
    }

}
