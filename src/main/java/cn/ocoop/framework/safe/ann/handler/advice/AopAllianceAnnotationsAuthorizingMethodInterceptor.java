package cn.ocoop.framework.safe.ann.handler.advice;

import cn.ocoop.framework.safe.ann.handler.*;
import com.google.common.collect.Lists;
import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import com.lanjoys.framework.safe.ann.handler.*;
import cn.ocoop.framework.safe.ann.handler.iface.AnnotationMethodInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class AopAllianceAnnotationsAuthorizingMethodInterceptor implements MethodInterceptor {
    private List<AnnotationMethodInterceptor> interceptors = Lists.newArrayList();

    public AopAllianceAnnotationsAuthorizingMethodInterceptor() {
        interceptors.add(new CaptchaAnnotationMethodInterceptor());
        interceptors.add(new RoleAnnotationMethodInterceptor());
        interceptors.add(new PermissionAnnotationMethodInterceptor());
        interceptors.add(new AuthenticatedAnnotationMethodInterceptor());
        interceptors.add(new UserAnnotationMethodInterceptor());
        interceptors.add(new GuestAnnotationMethodInterceptor());
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        assertAuthorized(invocation);
        return invocation.proceed();
    }

    protected void assertAuthorized(MethodInvocation methodInvocation) throws AuthorizingException {
        //default implementation just ensures no deny votes are cast:
        if (CollectionUtils.isNotEmpty(interceptors)) {
            for (AnnotationMethodInterceptor mi : interceptors) {
                if (mi.supports(methodInvocation)) {
                    mi.assertAuthorized(methodInvocation);
                }
            }
        }
    }


}
