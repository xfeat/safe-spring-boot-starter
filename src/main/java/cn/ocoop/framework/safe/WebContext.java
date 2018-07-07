package cn.ocoop.framework.safe;

import lombok.Getter;
import lombok.Setter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebContext {
    private static final ThreadLocal<Context> THREAD_LOCAL = new InheritableThreadLocal<>();


    public static void clear() {
        THREAD_LOCAL.remove();
    }

    public static Context get() {
        return THREAD_LOCAL.get();
    }

    public static void set(Context context) {
        THREAD_LOCAL.set(context);
    }

    @Getter
    @Setter
    public static class Context {
        private HttpServletRequest request;
        private HttpServletResponse response;
        private String sessionId;

        public Context(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }
    }
}
