package cn.ocoop.framework.safe;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import cn.ocoop.framework.safe.auth.service.AuthorizingService;
import cn.ocoop.framework.safe.utils.CookieUtils;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Data
public class SessionManager {

    public static SafeProperties safeProperties;
    public static StringRedisTemplate redisTemplate;
    public static AuthorizingService authorizingService;

    public static BoundHashOperations<String, String, String> getSession(String sessionId) {
        return redisTemplate.boundHashOps(getSessionkey(sessionId));
    }

    public static BoundHashOperations<String, String, String> getSession() {
        return redisTemplate.boundHashOps(getSessionkey(WebContext.get().getSessionId()));
    }

    public static List<BoundHashOperations<String, String, String>> getSession(long accountId) {
        Set<String> keys = redisTemplate.keys(getSessionMapKey(accountId, "*"));
        List<BoundHashOperations<String, String, String>> sessions = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(keys)) {
            for (String key : keys) {
                sessions.add(redisTemplate.boundHashOps(key));
            }
        }
        return sessions;
    }

    public static void setAttribute(long accountId, String key, String value) {
        for (BoundHashOperations<String, String, String> session : getSession(accountId)) {
            session.put(key, value);
        }
    }

    public static void removeAttribute(long accountId, String key) {
        for (BoundHashOperations<String, String, String> session : getSession(accountId)) {
            session.delete(key);
        }
    }


    public static String createSessionId() {
        return UUID.randomUUID().toString();
    }

    public static void setAttribute(String key, String value) {
        getSession().put(key, value);
    }

    public static String getAttribute(String key) {
        return getSession().get(key);
    }

    public static void removeAttribute(String key) {
        getSession().delete(key);
    }

    public static void logout(HttpServletResponse response) {
        getCurrentAccountId().ifPresent(accountId -> {
            redisTemplate.delete(Lists.newArrayList(
                    WebContext.get().getSessionId(),
                    getSessionMapKey(accountId, WebContext.get().getSessionId()),
                    getRoleKey(accountId),
                    getPmsKey(accountId)
            ));

            CookieUtils.clear(response, safeProperties.getSession().getSessionIdCookieName());
        });
    }

    private static BoundHashOperations<String, String, String> createSession(HttpServletResponse response, String sessionId, Long accountId) {
        BoundHashOperations<String, String, String> hash = redisTemplate.boundHashOps(getSessionkey(sessionId));

        hash.expire(2, TimeUnit.DAYS);
        hash.put(SafeProperties.SessionProperties.DEFAULT_SESSION_ID, sessionId);
        if (accountId != null) {
            hash.put("accountId", String.valueOf(accountId));
            redisTemplate.opsForValue().set(getSessionMapKey(accountId, sessionId), sessionId, 2, TimeUnit.DAYS);
        }

        CookieUtils.store(response, safeProperties.getSession().getSessionIdCookieName(), sessionId);
        return hash;
    }

    public static BoundHashOperations<String, String, String> createSession(HttpServletResponse response, String sessionId) {
        return createSession(response, sessionId, null);
    }

    /**
     * 创建已认证的会话
     *
     * @param response
     * @param accountId
     * @return
     */
    public static BoundHashOperations<String, String, String> createAuthenticatedSession(HttpServletResponse response, Long accountId) {
        return createSession(response, createSessionId(), accountId);
    }

    private static String getSessionkey(String sessionId) {
        return safeProperties.getSession().getSessionKeyPrefix() + sessionId;
    }

    private static String getSessionMapKey(long accountId, String sessionId) {
        return safeProperties.getSession().getSessionMapKeyPrefix() + accountId + ":" + sessionId;
    }

    public static boolean isLogin(String sessionId) {
        BoundHashOperations<String, String, String> session = getSession(sessionId);
        return StringUtils.isNotBlank(session.get("accountId"));
    }

    public static boolean isLogin() {
        BoundHashOperations<String, String, String> session = getSession();
        return StringUtils.isNotBlank(session.get("accountId"));
    }

    public static boolean touch(String sessionId) {
        Boolean expire = redisTemplate.expire(getSessionkey(sessionId), 2, TimeUnit.DAYS);
        if (BooleanUtils.isNotTrue(expire)) return false;

        if (!isLogin(sessionId)) return true;

        redisTemplate.opsForValue().set(getSessionMapKey(NumberUtils.toLong(getSession(sessionId).get("accountId")), sessionId), sessionId, 2, TimeUnit.DAYS);
        return true;
    }

    private static String getPmsKey(long accountId) {
        return "permissions:account_id:" + accountId;
    }

    private static String getRoleKey(long accountId) {
        return "roles:account_id:" + accountId;
    }

    public static Optional<Long> getCurrentAccountId() {
        BoundHashOperations<String, String, String> session = getSession(WebContext.get().getSessionId());
        if (StringUtils.isBlank(session.get("accountId"))) return Optional.empty();

        return Optional.of(NumberUtils.toLong(session.get("accountId")));
    }

    public static void clearRoleAndPermission() {
        redisTemplate.delete(safeProperties.getSession().getPermissionKey());
    }

    public static void clearRoleAndPermission(long accountId) {
        redisTemplate.opsForHash().delete(
                safeProperties.getSession().getPermissionKey(),
                getPmsKey(accountId),
                getRoleKey(accountId)
        );
    }

    public static void clearRole(long accountId) {
        redisTemplate.opsForHash().delete(
                safeProperties.getSession().getPermissionKey(),
                getRoleKey(accountId)
        );
    }

    public static void clearPermission(long accountId) {
        redisTemplate.opsForHash().delete(
                safeProperties.getSession().getPermissionKey(),
                getPmsKey(accountId)
        );
    }


    @SuppressWarnings("unchecked")
    public static List<String> getPermission() {
        Optional<Long> AccountIdOptional = getCurrentAccountId();
        if (!AccountIdOptional.isPresent()) {
            return Lists.newArrayList();
        }

        BoundHashOperations<String, String, String> pmsStore = redisTemplate.boundHashOps(safeProperties.getSession().getPermissionKey());

        String pmsKey = getPmsKey(AccountIdOptional.get());
        if (BooleanUtils.isNotTrue(pmsStore.hasKey(pmsKey))) {
            List<String> pms = authorizingService.listPermission(AccountIdOptional.get());
            if (CollectionUtils.isEmpty(pms)) {
                pms = Lists.newArrayList();
            }
            pmsStore.put(pmsKey, JSON.toJSONString(pms));
        }

        return JSON.parseObject(pmsStore.get(pmsKey), List.class);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRole() {
        Optional<Long> AccountIdOptional = getCurrentAccountId();
        if (!AccountIdOptional.isPresent()) {
            return Lists.newArrayList();
        }

        BoundHashOperations<String, String, String> roleStore = redisTemplate.boundHashOps(safeProperties.getSession().getPermissionKey());

        String roleKey = getRoleKey(AccountIdOptional.get());
        if (BooleanUtils.isNotTrue(roleStore.hasKey(roleKey))) {
            List<String> pms = authorizingService.listRole(AccountIdOptional.get());
            if (CollectionUtils.isEmpty(pms)) {
                pms = Lists.newArrayList();
            }
            roleStore.put(roleKey, JSON.toJSONString(pms));
        }

        return JSON.parseObject(roleStore.get(roleKey), List.class);
    }


    public static boolean hasRole(String... role) {
        return getRole().containsAll(Arrays.asList(role));
    }

    public static boolean hasAnyRole(String... role) {
        List<String> role1 = getRole();
        return Stream.of(role).anyMatch(role1::contains);
    }

    public static boolean hasPermission(String... pms) {
        return getPermission().containsAll(Arrays.asList(pms));
    }

    public static boolean hasAnyPermission(String... pms) {
        List<String> pms1 = getPermission();
        return Stream.of(pms).anyMatch(pms1::contains);
    }

}
