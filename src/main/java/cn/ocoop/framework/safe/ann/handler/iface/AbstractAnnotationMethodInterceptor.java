package cn.ocoop.framework.safe.ann.handler.iface;

import cn.ocoop.framework.safe.SessionManager;
import cn.ocoop.framework.safe.ex.InvalidConfigStateException;
import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import cn.ocoop.framework.safe.ex.authz.InvalidSessionStateException;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Slf4j
public abstract class AbstractAnnotationMethodInterceptor implements AnnotationMethodInterceptor {
    protected Class<? extends Annotation> annotationClass;

    public AbstractAnnotationMethodInterceptor() {
    }

    public AbstractAnnotationMethodInterceptor(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    @Override
    public boolean supports(MethodInvocation mi) {
        if (annotationClass == null)
            throw new InvalidConfigStateException("incorrect AnnotationMethodIntercept configured");
        return getAnnotation(mi, annotationClass) != null;
    }

    @Override
    public void assertAuthorized(MethodInvocation methodInvocation) throws AuthorizingException {
        Annotation annotation = getAnnotation(methodInvocation, annotationClass);
        try {
            assertSessionState(methodInvocation, annotation);
            assertAuth(methodInvocation, annotation);
        } catch (AuthorizingException e) {
            log.error("lacks permission:{}{}", methodInvocation.getMethod(), e.getMessage());
            throw e;
        }
    }

    protected abstract void assertAuth(MethodInvocation methodInvocation, Annotation annotation) throws AuthorizingException;

    protected void assertSessionState(MethodInvocation methodInvocation, Annotation annotation) throws AuthorizingException {
        if (!SessionManager.isValidSessionState()) throw new InvalidSessionStateException("invalid session state!");
    }

    protected Annotation getAnnotation(MethodInvocation mi, Class<? extends Annotation> annotationClass) {
        Method m = mi.getMethod();

        Annotation a = AnnotationUtils.findAnnotation(m, annotationClass);
        if (a != null) return a;

        //The MethodInvocation's method object could be a method defined in an interface.
        //However, if the annotation existed in the interface's implementation (and not
        //the interface itself), it won't be on the above method object.  Instead, we need to
        //acquire the method representation from the targetClass and check directly on the
        //implementation itself:
        Class<?> targetClass = mi.getThis().getClass();
        m = ClassUtils.getMostSpecificMethod(m, targetClass);
        a = AnnotationUtils.findAnnotation(m, annotationClass);
        if (a != null) return a;
        // See if the class has the same annotation
        return AnnotationUtils.findAnnotation(mi.getThis().getClass(), annotationClass);
    }
}
