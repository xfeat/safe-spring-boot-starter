package cn.ocoop.framework.safe.ex.authc;

public class UnknownAccountException extends AuthenticatingException {
    public UnknownAccountException(String message) {
        super(message);
    }
}
