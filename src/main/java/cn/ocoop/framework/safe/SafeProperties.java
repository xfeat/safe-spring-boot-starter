package cn.ocoop.framework.safe;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import static cn.ocoop.framework.safe.SafeProperties.PREFIX;


@RefreshScope
@Data
@ConfigurationProperties(prefix = PREFIX)
public class SafeProperties {
    public static final String PREFIX = "safe.session";

    public static final String DEFAULT_SESSION_ID = "_id";
    public static final String DEFAULT_SYS_PMS_REFRESH_TIME = "safe:refresh:pms";

    private String sessionIdCookieName = "sessionId";
    private String sessionKeyPrefix = "safe:session:";
    private String sessionMapKeyPrefix = "safe:session-map:";
    private String permissionKey = "safe:pms";
    private String permissionRefreshKey = DEFAULT_SYS_PMS_REFRESH_TIME;

}
