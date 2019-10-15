package cn.ocoop.framework.safe;

import cn.ocoop.framework.safe.auth.service.AuthorizingService;
import cn.ocoop.framework.safe.utils.CookieUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Data
public class SessionManager {

    private static final String SESSION_PMS_REFRESH_ATTR_KEY = "_LAST_PMS_REFRESH_TIME";
    private static final String DEFAULT_STATE_ATTR_KEY = "_STATE";
    private static final String INVALID_STATE_ATTR_VALUE = "_INVALID";
    public static SafeProperties safeProperties;
    public static CaptchaProperties captchaProperties;
    public static StringRedisTemplate redisTemplate;
    public static AuthorizingService authorizingService;

    public static BoundHashOperations<String, String, String> getSession(String sessionId) {
        return redisTemplate.boundHashOps(getSessionkey(sessionId));
    }

    public static BoundHashOperations<String, String, String> getSession() {

        if (WebContext.get().getSessionId() == null) {
            String sessionId = createSessionId();
            SessionManager.createSession(WebContext.get().getResponse(), sessionId);
            WebContext.get().setSessionId(sessionId);
            log.info("new session[{}] is created!", sessionId);
        }

        return redisTemplate.boundHashOps(getSessionkey(WebContext.get().getSessionId()));
    }

    public static List<BoundHashOperations<String, String, String>> getSession(long accountId) {
        Set<String> keys = redisTemplate.keys(getSessionMapKey(accountId, "*"));

        List<BoundHashOperations<String, String, String>> sessions = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(keys)) {
            for (String key : keys) {
                sessions.add(getSession(redisTemplate.opsForValue().get(key)));
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

            CookieUtils.clear(response, safeProperties.getSessionIdCookieName());
        });
    }

    private static BoundHashOperations<String, String, String> createSession(HttpServletResponse response, String sessionId, Long accountId) {
        BoundHashOperations<String, String, String> hash = redisTemplate.boundHashOps(getSessionkey(sessionId));

        hash.put(SafeProperties.DEFAULT_SESSION_ID, sessionId);
        hash.expire(2, TimeUnit.DAYS);
        if (accountId != null) {
            hash.put("accountId", String.valueOf(accountId));
            redisTemplate.opsForValue().set(getSessionMapKey(accountId, sessionId), sessionId, 2, TimeUnit.DAYS);
        }

        CookieUtils.store(response, safeProperties.getSessionIdCookieName(), sessionId);
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
        BoundHashOperations<String, String, String> lastSession = getSession();

        BoundHashOperations<String, String, String> session = createSession(response, createSessionId(), accountId);
        Map<String, String> entries = lastSession.entries();
        if (entries != null) {
            for (String key : entries.keySet()) {
                if ("accountId".equals(key) || SafeProperties.DEFAULT_SESSION_ID.equals(key) || entries.get(key) == null) {
                    continue;
                }

                session.put(key, entries.get(key));
            }
        }

        clearLastSession(accountId, WebContext.get().getSessionId());
        return session;
    }

    private static void clearLastSession(Long accountId, String sessionId) {
        redisTemplate.delete(getSessionkey(sessionId));
        if (accountId != null) {
            redisTemplate.delete(getSessionMapKey(accountId, sessionId));
        }
    }

    private static String getSessionkey(String sessionId) {
        return safeProperties.getSessionKeyPrefix() + sessionId;
    }

    private static String getSessionMapKey(long accountId, String sessionId) {
        return safeProperties.getSessionMapKeyPrefix() + accountId + ":" + sessionId;
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

        if (isLogin(sessionId)) {
            redisTemplate.opsForValue().set(getSessionMapKey(NumberUtils.toLong(getSession(sessionId).get("accountId")), sessionId), sessionId, 2, TimeUnit.DAYS);
        }

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

    public static void clearAllRoleAndPermission() {
        redisTemplate.delete(safeProperties.getPermissionKey());
    }

    public static void clearRoleAndPermission(long accountId) {
        clearRole(accountId);
        clearPermission(accountId);
    }

    public static void clearRoleAndPermission() {
        getCurrentAccountId().ifPresent(accountId -> {
            clearRole(accountId);
            clearPermission(accountId);
        });
    }

    public static void clearRole(long accountId) {
        redisTemplate.opsForHash().delete(
                safeProperties.getPermissionKey(),
                getRoleKey(accountId)
        );
    }

    public static void clearPermission(long accountId) {
        redisTemplate.opsForHash().delete(
                safeProperties.getPermissionKey(),
                getPmsKey(accountId)
        );
    }

    public static void refreshCachedRoleAndPermissionIfNecessary() {
        String cachedRefreshTime = getAttribute(SESSION_PMS_REFRESH_ATTR_KEY);
        if (cachedRefreshTime == null) return;

        String refreshTime = redisTemplate.opsForValue().get(safeProperties.getPermissionRefreshKey());
        if (refreshTime == null || Long.parseLong(cachedRefreshTime) > Long.parseLong(refreshTime)) return;

        clearRoleAndPermission();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getPermission() {
        Optional<Long> AccountIdOptional = getCurrentAccountId();
        if (!AccountIdOptional.isPresent()) {
            return Lists.newArrayList();
        }

        BoundHashOperations<String, String, String> pmsStore = redisTemplate.boundHashOps(safeProperties.getPermissionKey());

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

        BoundHashOperations<String, String, String> roleStore = redisTemplate.boundHashOps(safeProperties.getPermissionKey());

        String roleKey = getRoleKey(AccountIdOptional.get());
        if (BooleanUtils.isNotTrue(roleStore.hasKey(roleKey))) {
            List<String> pms = authorizingService.listRole(AccountIdOptional.get());
            if (CollectionUtils.isEmpty(pms)) {
                pms = Lists.newArrayList();
            }
            roleStore.put(roleKey, JSON.toJSONString(pms));
            roleStore.put(SESSION_PMS_REFRESH_ATTR_KEY, String.valueOf(Instant.now().toEpochMilli()));
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


    public static void refreshAllRoleAndPermission(String... key) {
        if (ArrayUtils.isEmpty(key)) {
            redisTemplate.opsForValue().set(safeProperties.getPermissionRefreshKey(), String.valueOf(Instant.now().toEpochMilli()));
            return;
        }

        Stream.of(key).forEach(k -> redisTemplate.opsForValue().set(k, String.valueOf(Instant.now().toEpochMilli())));
    }

    public static void invalid(long accountId) {
        setAttribute(accountId, DEFAULT_STATE_ATTR_KEY, INVALID_STATE_ATTR_VALUE);
    }

    public static void valid(long accountId) {
        removeAttribute(accountId, DEFAULT_STATE_ATTR_KEY);
    }

    public static boolean isValidSessionState() {
        return !INVALID_STATE_ATTR_VALUE.equals(getAttribute(DEFAULT_STATE_ATTR_KEY));
    }

}
