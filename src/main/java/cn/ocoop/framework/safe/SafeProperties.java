package cn.ocoop.framework.safe;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static cn.ocoop.framework.safe.SafeProperties.PREFIX;


@Data
@ConfigurationProperties(prefix = PREFIX)
public class SafeProperties {
    public static final String PREFIX = "safe";
    private SessionProperties session = new SessionProperties();
    private CaptchaProperties captcha = new CaptchaProperties();

    @Data
    public static class SessionProperties {
        public static final String DEFAULT_SESSION_ID = "_id";
        public static final String DEFAULT_SESSION_CAPTCHA = "_captcha";
        public static final String DEFAULT_SESSION_LOCK = "_captcha";
        public static final String DEFAULT_NULL_PMS = "";

        private String sessionIdCookieName = "sessionId";
        private String sessionKeyPrefix = "safe:session:";
        private String sessionMapKeyPrefix = "safe:session-map:";
        private String permissionKey = "safe:pms";
    }

    @Data
    public static class CaptchaProperties {
        private int width = 100;
        private int height = 40;
        private int length = 4;
    }
}
