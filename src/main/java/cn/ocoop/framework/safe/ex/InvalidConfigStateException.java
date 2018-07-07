package cn.ocoop.framework.safe.ex;

public class InvalidConfigStateException extends RuntimeException {
    public InvalidConfigStateException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return null;
    }
}
