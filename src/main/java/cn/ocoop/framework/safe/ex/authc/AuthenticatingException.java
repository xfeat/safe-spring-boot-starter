package cn.ocoop.framework.safe.ex.authc;

public class AuthenticatingException extends Exception {
    public AuthenticatingException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
