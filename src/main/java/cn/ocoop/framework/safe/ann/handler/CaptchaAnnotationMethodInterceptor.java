package cn.ocoop.framework.safe.ann.handler;

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
    public static final String DEFAULT_SESSION_CAPTCHA = "_captcha";
    public static final String X_CAPTCHA = "X-Captcha";

    public CaptchaAnnotationMethodInterceptor() {
        super(RequiresCaptcha.class);
    }

    @Override
    protected void assertAuth(MethodInvocation methodInvocation, Annotation annotation) throws AuthorizingException {
        String captcha = SessionManager.getAttribute(DEFAULT_SESSION_CAPTCHA);
        if (StringUtils.isBlank(captcha)) throw new InvalidCaptchaException("captcha required, check it out first!");

        SessionManager.removeAttribute(DEFAULT_SESSION_CAPTCHA);
        String inputCaptcha = WebContext.get().getRequest().getHeader(X_CAPTCHA);
        if (!captcha.equalsIgnoreCase(inputCaptcha)) {
            throw new InvalidCaptchaException("invalid captcha input!");
        }
    }
}
