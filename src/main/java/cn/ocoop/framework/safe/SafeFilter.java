package cn.ocoop.framework.safe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class SafeFilter extends OncePerRequestFilter {
    public static final int DEFAULT_ORDER = Integer.MIN_VALUE + 50;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        WebContext.set(new WebContext.Context(request, response));

        SessionManager.touch();

        filterChain.doFilter(request, response);
    }
}
