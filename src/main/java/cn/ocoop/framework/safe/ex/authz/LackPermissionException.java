package cn.ocoop.framework.safe.ex.authz;

/**
 * 无权限
 */
public class LackPermissionException extends AuthorizingException {
    public LackPermissionException(String message) {
        super(message);
    }
}
