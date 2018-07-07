package cn.ocoop.framework.safe.ann.handler;

import cn.ocoop.framework.safe.ann.handler.iface.AbstractAnnotationMethodInterceptor;
import cn.ocoop.framework.safe.ann.RequiresUser;
import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;

public class UserAnnotationMethodInterceptor extends AbstractAnnotationMethodInterceptor {
    public UserAnnotationMethodInterceptor() {
        super(RequiresUser.class);
    }

    @Override
    protected void assertAuth(MethodInvocation methodInvocation, Annotation annotation) throws AuthorizingException {
    }
}
