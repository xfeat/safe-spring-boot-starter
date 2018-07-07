package cn.ocoop.framework.safe.ann.handler;

import cn.ocoop.framework.safe.SessionManager;
import cn.ocoop.framework.safe.ann.handler.iface.AbstractAnnotationMethodInterceptor;
import com.google.common.base.Joiner;
import cn.ocoop.framework.safe.ann.Logical;
import cn.ocoop.framework.safe.ann.RequiresRoles;
import cn.ocoop.framework.safe.ex.authz.AuthorizingException;
import cn.ocoop.framework.safe.ex.authz.LackPermissionException;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;

public class RoleAnnotationMethodInterceptor extends AbstractAnnotationMethodInterceptor {
    public RoleAnnotationMethodInterceptor() {
        super(RequiresRoles.class);
    }

    @Override
    protected void assertAuth(MethodInvocation methodInvocation, Annotation annotation) throws AuthorizingException {
        RequiresRoles requiresRoles = (RequiresRoles) annotation;

        if (requiresRoles.logical() == Logical.AND) {
            if (!SessionManager.hasRole(requiresRoles.value())) {
                throw new LackPermissionException("无权限,需要" + Joiner.on(",").join(requiresRoles.value()));
            }
            return;
        }

        if (!SessionManager.hasAnyRole(requiresRoles.value())) {
            throw new LackPermissionException("无权限,需要" + Joiner.on(",").join(requiresRoles.value()));
        }
    }
}
