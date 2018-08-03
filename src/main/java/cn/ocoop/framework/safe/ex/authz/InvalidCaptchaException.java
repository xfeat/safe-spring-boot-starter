package cn.ocoop.framework.safe.ex.authz;

/**
 * 无权限
 */
public class InvalidCaptchaException extends AuthorizingException {
    public InvalidCaptchaException(String message) {
        super(message);
    }
}
