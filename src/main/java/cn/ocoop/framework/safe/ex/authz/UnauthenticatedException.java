package cn.ocoop.framework.safe.ex.authz;

/**
 * 未登录
 */
public class UnauthenticatedException extends AuthorizingException {
    public UnauthenticatedException(String message) {
        super(message);
    }
}
