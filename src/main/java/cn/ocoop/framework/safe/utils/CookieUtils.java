package cn.ocoop.framework.safe.utils;

import org.apache.commons.lang3.ArrayUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

public class CookieUtils {

    public static void store(HttpServletResponse response, String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    public static void clear(HttpServletResponse response, String key) {
        Cookie cookie = new Cookie(key, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public static Optional<Cookie> get(HttpServletRequest request, String cookieName) {
        if (ArrayUtils.isEmpty(request.getCookies())) return Optional.empty();

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return Optional.of(cookie);
            }
        }
        return Optional.empty();
    }
}
