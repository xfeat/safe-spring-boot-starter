package cn.ocoop.framework.safe.ex.authc;

public class AccountLockedException extends AuthenticatingException {
    public AccountLockedException(String message) {
        super(message);
    }
}
