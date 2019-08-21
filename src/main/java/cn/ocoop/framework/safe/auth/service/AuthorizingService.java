package cn.ocoop.framework.safe.auth.service;


import java.util.List;

public interface AuthorizingService {
    List<String> listRole(long accountId);

    List<String> listPermission(long accountId);
}
