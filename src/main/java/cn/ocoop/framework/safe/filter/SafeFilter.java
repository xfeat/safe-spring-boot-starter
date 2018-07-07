package cn.ocoop.framework.safe.filter;

import cn.ocoop.framework.safe.SessionManager;
import cn.ocoop.framework.safe.WebContext;
import cn.ocoop.framework.safe.utils.CookieUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("SafeFilter:请求地址:{}{}", request.getRequestURL(), StringUtils.isNotBlank(request.getQueryString()) ? "?" + request.getQueryString() : "");
        WebContext.clear();
        WebContext.set(new WebContext.Context(request, response));

        Optional<Cookie> cookie = CookieUtils.get(request, SessionManager.safeProperties.getSession().getSessionIdCookieName());
        if (!cookie.isPresent()) {
            log.info("创建新的会话");
            String sessionId = createSession(response);
            WebContext.get().setSessionId(sessionId);
            filterChain.doFilter(request, response);
            return;
        }

        String sessionId = cookie.get().getValue();
        if (!SessionManager.touch(sessionId)) {
            log.info("会话:{}超时,重新创建会话", sessionId);
            sessionId = createSession(response);
        }
        WebContext.get().setSessionId(sessionId);
        filterChain.doFilter(request, response);
    }

    private String createSession(HttpServletResponse response) {
        String sessionId = SessionManager.createSessionId();
        SessionManager.createSession(response, sessionId);
        return sessionId;
    }
}
