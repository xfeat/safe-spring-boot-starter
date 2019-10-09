package cn.ocoop.framework.safe.ex.authz;

public class InvalidSessionStateException extends AuthorizingException {
    public InvalidSessionStateException(String message) {
        super(message);
    }
}
