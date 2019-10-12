package cn.ocoop.framework.safe;

import cn.ocoop.framework.safe.utils.CookieUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Slf4j
public class SafeFilter extends OncePerRequestFilter {
    public static final int DEFAULT_ORDER = Integer.MIN_VALUE + 50;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        WebContext.clear();
        WebContext.set(new WebContext.Context(request, response));

        Optional<Cookie> cookie = CookieUtils.get(request, SessionManager.safeProperties.getSessionIdCookieName());
        if (!cookie.isPresent()) {
            filterChain.doFilter(request, response);
            return;
        }

        String sessionId = cookie.get().getValue();
        if (SessionManager.touch(sessionId)) {
            WebContext.get().setSessionId(sessionId);
        }
        filterChain.doFilter(request, response);
    }
}
