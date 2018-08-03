package cn.ocoop.framework.safe.ann.handler;

import cn.ocoop.framework.safe.SessionManager;
import cn.ocoop.framework.safe.ann.RequiresAuthentication;
import cn.ocoop.framework.safe.ann.handler.iface.AbstractAnnotationMethodInterceptor;
import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import cn.ocoop.framework.safe.ex.authz.UnauthenticatedException;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;

public class AuthenticatedAnnotationMethodInterceptor extends AbstractAnnotationMethodInterceptor {
    public AuthenticatedAnnotationMethodInterceptor() {
        super(RequiresAuthentication.class);
    }

    public AuthenticatedAnnotationMethodInterceptor(Class<? extends Annotation> annotationClass) {
        super(annotationClass);
    }

    @Override
    protected void assertAuth(MethodInvocation methodInvocation, Annotation annotation) throws AuthorizingException {
        if (!SessionManager.isLogin()) throw new UnauthenticatedException("未登录");
    }
}
