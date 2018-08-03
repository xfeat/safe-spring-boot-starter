package cn.ocoop.framework.safe.ex.authz;

public class AuthorizingException extends RuntimeException {
    public AuthorizingException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
