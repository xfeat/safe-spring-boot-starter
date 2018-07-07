package cn.ocoop.framework.safe.ann.handler;

import cn.ocoop.framework.safe.SessionManager;
import com.google.common.base.Joiner;
import cn.ocoop.framework.safe.ann.Logical;
import cn.ocoop.framework.safe.ann.RequiresPermissions;
import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import cn.ocoop.framework.safe.ex.authz.LackPermissionException;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;

public class PermissionAnnotationMethodInterceptor extends AuthenticatedAnnotationMethodInterceptor {
    public PermissionAnnotationMethodInterceptor() {
        super(RequiresPermissions.class);
    }

    @Override
    protected void assertAuth(MethodInvocation methodInvocation, Annotation annotation) throws AuthorizingException {
        RequiresPermissions requiresPermissions = (RequiresPermissions) annotation;
        String[] requiredPmsCode = requiresPermissions.value();
        if (ArrayUtils.isEmpty(requiredPmsCode)) {
            String defaultPmsCode = "";
            RequestMapping controllerMapping = AnnotationUtils.findAnnotation(methodInvocation.getThis().getClass(), RequestMapping.class);
            if (controllerMapping != null && ArrayUtils.isNotEmpty(controllerMapping.value())) {
                defaultPmsCode += Joiner.on(",").join(controllerMapping.value());
            }

            RequestMapping methodMapping = AnnotationUtils.findAnnotation(methodInvocation.getMethod(), RequestMapping.class);
            if (methodMapping != null && ArrayUtils.isNotEmpty(methodMapping.value())) {
                defaultPmsCode += Joiner.on(",").join(methodMapping.value());
            }

            if (StringUtils.isNotBlank(defaultPmsCode)) {
                requiredPmsCode = new String[]{defaultPmsCode};
            }
        }

        if (requiresPermissions.logical() == Logical.AND) {
            if (!SessionManager.hasPermission(requiredPmsCode)) {
                throw new LackPermissionException("无权限,需要" + Joiner.on(",").join(requiredPmsCode));
            }
            return;
        }

        if (!SessionManager.hasAnyPermission(requiredPmsCode)) {
            throw new LackPermissionException("无权限,需要" + Joiner.on(",").join(requiredPmsCode));
        }

    }
}
