package cn.ocoop.framework.safe.ann.handler;


import cn.ocoop.framework.safe.ann.RequiresGuest;
import cn.ocoop.framework.safe.ann.handler.iface.AbstractAnnotationMethodInterceptor;
import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;

public class GuestAnnotationMethodInterceptor extends AbstractAnnotationMethodInterceptor {
    public GuestAnnotationMethodInterceptor() {
        super(RequiresGuest.class);
    }

    @Override
    protected void assertAuth(MethodInvocation methodInvocation, Annotation annotation) throws AuthorizingException {
    }
}
