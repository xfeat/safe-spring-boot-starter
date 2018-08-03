package cn.ocoop.framework.safe.ex.authc;

public class IncorrectCredentialsException extends AuthenticatingException {
    public IncorrectCredentialsException(String message) {
        super(message);
    }
}
