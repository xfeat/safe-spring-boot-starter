package cn.ocoop.framework.safe;

import cn.ocoop.framework.safe.auth.service.AuthorizingService;
import cn.ocoop.framework.safe.utils.CookieUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Data
public class SessionManager {

    private static final String ATTR_KEY_LAST_PMS_REFRESH_TIME = SessionManager.class.getName().concat(".LAST_PMS_REFRESH_TIME");
    private static final String ATTR_KEY_STATE = SessionManager.class.getName().concat(".STATE");
    private static final String ATTR_KEY_ACCOUNT_ID = SessionManager.class.getName().concat(".ACCOUNT_ID");
    private static final String ATTR_KEY_ID = SessionManager.class.getName().concat(".SESSION_ID");
    private static final String INVALID_STATE_ATTR_VALUE = SessionManager.class.getName().concat(".INVALID");
    public static SafeProperties safeProperties;
    public static CaptchaProperties captchaProperties;
    public static StringRedisTemplate redisTemplate;
    public static AuthorizingService authorizingService;
    private static Set<String> PRE_DEFINED_ATTR_KEY = Sets.newHashSet(ATTR_KEY_LAST_PMS_REFRESH_TIME, ATTR_KEY_STATE, ATTR_KEY_ACCOUNT_ID, ATTR_KEY_ID);

    public static BoundHashOperations<String, String, String> getSession(String sessionId) {
        BoundHashOperations<String, String, String> session = redisTemplate.boundHashOps(getSessionkey(sessionId));
        session.put(ATTR_KEY_ID, sessionId);
        return session;
    }

    public static Optional<BoundHashOperations<String, String, String>> getSession(boolean create) {

        if (WebContext.get().getSessionId() == null && create) {
            createSession(WebContext.get().getResponse(), createSessionId(), null);
            log.info("new session[{}] is created!", WebContext.get().getSessionId());
        }

        if (WebContext.get().getSessionId() == null) return Optional.empty();
        return Optional.of(getSession(WebContext.get().getSessionId()));
    }

    public static List<BoundHashOperations<String, String, String>> getSession(long accountId) {
        Set<String> keys = redisTemplate.keys(getAccountRefSessionKey(accountId, "*"));

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
        getSession(true).get().put(key, value);
    }

    public static String getAttribute(String key) {
        return getSession(true).get().get(key);
    }

    public static void removeAttribute(String key) {
        getSession(true).get().delete(key);
    }

    public static void logout() {
        getCurrentAccountId().ifPresent(accountId -> logout(accountId, WebContext.get().getSessionId()));
    }

    public static void logout(long accountId, String sessionId) {
        redisTemplate.delete(Lists.newArrayList(
                getSessionkey(sessionId),
                getAccountRefSessionKey(accountId, sessionId),
                getRoleKey(accountId),
                getPmsKey(accountId)
        ));
    }

    private static BoundHashOperations<String, String, String> createSession(HttpServletResponse response, String sessionId, Long accountId) {
        BoundHashOperations<String, String, String> session = redisTemplate.boundHashOps(getSessionkey(sessionId));
        session.expire(safeProperties.getTimeout(), TimeUnit.SECONDS);
        session.put(ATTR_KEY_ID, sessionId);
        if (accountId != null) {
            session.put(ATTR_KEY_ACCOUNT_ID, String.valueOf(accountId));
            redisTemplate.opsForValue().set(getAccountRefSessionKey(accountId, sessionId), sessionId, safeProperties.getTimeout(), TimeUnit.SECONDS);
        }

        CookieUtils.store(response, safeProperties.getSessionIdCookieName(), sessionId);

        WebContext.get().setSessionId(sessionId);

        return session;
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
        Optional<BoundHashOperations<String, String, String>> lastSession = getSession(false);

        BoundHashOperations<String, String, String> session = createSession(response, createSessionId(), accountId);
        if (!lastSession.isPresent()) return session;

        Map<String, String> entries = lastSession.get().entries();
        if (entries != null) {
            for (String key : entries.keySet()) {
                if (PRE_DEFINED_ATTR_KEY.contains(key) || entries.get(key) == null) continue;

                session.put(key, entries.get(key));
            }
        }

        logout(accountId, WebContext.get().getSessionId());
        return session;
    }

    private static String getSessionkey(String sessionId) {
        return safeProperties.getSessionKeyPrefix() + sessionId;
    }

    private static String getAccountRefSessionKey(long accountId, String sessionId) {
        return safeProperties.getSessionMapKeyPrefix() + accountId + ":" + sessionId;
    }

    public static boolean isLogin(String sessionId) {
        BoundHashOperations<String, String, String> session = getSession(sessionId);
        return StringUtils.isNotBlank(session.get(ATTR_KEY_ACCOUNT_ID));
    }

    public static boolean isLogin() {
        Optional<BoundHashOperations<String, String, String>> session = getSession(false);
        return session.filter(s -> StringUtils.isNotBlank(s.get(ATTR_KEY_ACCOUNT_ID))).isPresent();

    }

    public static void touch() {
        Optional<Cookie> cookie = CookieUtils.get(WebContext.get().getRequest(), safeProperties.getSessionIdCookieName());
        if (!cookie.isPresent()) return;

        Boolean expire = redisTemplate.expire(getSessionkey(cookie.get().getValue()), safeProperties.getTimeout(), TimeUnit.SECONDS);
        if (BooleanUtils.isNotTrue(expire)) return;

        WebContext.get().setSessionId(cookie.get().getValue());

        Optional<Long> currentAccountId = getCurrentAccountId();

        if (!currentAccountId.isPresent()) return;

        redisTemplate.expire(getAccountRefSessionKey(currentAccountId.get(), cookie.get().getValue()), safeProperties.getTimeout(), TimeUnit.SECONDS);
    }

    private static String getPmsKey(long accountId) {
        return "permissions:account_id:" + accountId;
    }

    private static String getRoleKey(long accountId) {
        return "roles:account_id:" + accountId;
    }

    public static Optional<Long> getCurrentAccountId() {
        BoundHashOperations<String, String, String> session = getSession(WebContext.get().getSessionId());
        String accountId = session.get(ATTR_KEY_ACCOUNT_ID);
        if (StringUtils.isBlank(accountId)) return Optional.empty();

        return Optional.of(Long.parseLong(accountId));
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
        String cachedRefreshTime = getAttribute(ATTR_KEY_LAST_PMS_REFRESH_TIME);
        if (cachedRefreshTime == null) return;

        String refreshTime = redisTemplate.opsForValue().get(safeProperties.getPermissionRefreshKey());
        if (refreshTime == null || Long.parseLong(cachedRefreshTime) > Long.parseLong(refreshTime)) return;

        clearRoleAndPermission();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getPermission() {
        Optional<Long> accountIdOptional = getCurrentAccountId();
        if (!accountIdOptional.isPresent()) return Lists.newArrayList();

        BoundHashOperations<String, String, String> pmsStore = redisTemplate.boundHashOps(safeProperties.getPermissionKey());

        String pmsKey = getPmsKey(accountIdOptional.get());
        if (BooleanUtils.isNotTrue(pmsStore.hasKey(pmsKey))) {
            List<String> pms = authorizingService.listPermission(accountIdOptional.get());
            if (CollectionUtils.isEmpty(pms)) {
                pms = Lists.newArrayList();
            }
            pmsStore.put(pmsKey, JSON.toJSONString(pms));
            pmsStore.put(ATTR_KEY_LAST_PMS_REFRESH_TIME, String.valueOf(Instant.now().toEpochMilli()));
        }

        return JSON.parseObject(pmsStore.get(pmsKey), List.class);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRole() {
        Optional<Long> accountIdOptional = getCurrentAccountId();
        if (!accountIdOptional.isPresent()) return Lists.newArrayList();

        BoundHashOperations<String, String, String> roleStore = redisTemplate.boundHashOps(safeProperties.getPermissionKey());

        String roleKey = getRoleKey(accountIdOptional.get());
        if (BooleanUtils.isNotTrue(roleStore.hasKey(roleKey))) {
            List<String> pms = authorizingService.listRole(accountIdOptional.get());
            if (CollectionUtils.isEmpty(pms)) {
                pms = Lists.newArrayList();
            }
            roleStore.put(roleKey, JSON.toJSONString(pms));
            roleStore.put(ATTR_KEY_LAST_PMS_REFRESH_TIME, String.valueOf(Instant.now().toEpochMilli()));
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
        setAttribute(accountId, ATTR_KEY_STATE, INVALID_STATE_ATTR_VALUE);
    }

    public static void valid(long accountId) {
        removeAttribute(accountId, ATTR_KEY_STATE);
    }

    public static boolean isValidSessionState() {
        return !INVALID_STATE_ATTR_VALUE.equals(getAttribute(ATTR_KEY_STATE));
    }

}
