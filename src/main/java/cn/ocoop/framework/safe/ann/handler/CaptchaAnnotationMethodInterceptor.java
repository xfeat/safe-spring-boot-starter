package cn.ocoop.framework.safe.ann.handler;

import cn.ocoop.framework.safe.SafeProperties;
import cn.ocoop.framework.safe.SessionManager;
import cn.ocoop.framework.safe.WebContext;
import cn.ocoop.framework.safe.ann.RequiresCaptcha;
import cn.ocoop.framework.safe.ann.handler.iface.AbstractAnnotationMethodInterceptor;
import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import cn.ocoop.framework.safe.ex.authz.InvalidCaptchaException;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;

public class CaptchaAnnotationMethodInterceptor extends AbstractAnnotationMethodInterceptor {

    public static final String X_CAPTCHA = "X-Captcha";

    public CaptchaAnnotationMethodInterceptor() {
        super(RequiresCaptcha.class);
    }

    @Override
    protected void assertAuth(MethodInvocation methodInvocation, Annotation annotation) throws AuthorizingException {
        String captcha = SessionManager.getAttribute(SafeProperties.SessionProperties.DEFAULT_SESSION_CAPTCHA);
        if (StringUtils.isBlank(captcha)) throw new InvalidCaptchaException("请获取验证码");

        SessionManager.removeAttribute(SafeProperties.SessionProperties.DEFAULT_SESSION_CAPTCHA);
        String inputCaptcha = WebContext.get().getRequest().getHeader(X_CAPTCHA);
        if (!captcha.equalsIgnoreCase(inputCaptcha)) {
            throw new InvalidCaptchaException("验证码不正确");
        }
    }
}
