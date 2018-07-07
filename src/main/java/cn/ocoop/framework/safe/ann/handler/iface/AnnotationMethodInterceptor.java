package cn.ocoop.framework.safe.ann.handler.iface;

import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import org.aopalliance.intercept.MethodInvocation;

public interface AnnotationMethodInterceptor {
    boolean supports(MethodInvocation mi);

    void assertAuthorized(MethodInvocation methodInvocation) throws AuthorizingException;
}
