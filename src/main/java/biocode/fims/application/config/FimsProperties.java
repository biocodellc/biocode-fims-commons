package biocode.fims.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author rjewing
 */
@Component
public class FimsProperties {
    private final Environment env;

    @Autowired
    public FimsProperties(Environment env) {
        this.env = env;
    }

    public String appRoot() {
        return env.getRequiredProperty("appRoot");
    }

    public String divider() {
        return env.getRequiredProperty("divider");
    }

    public String publisher() {
        return env.getRequiredProperty("publisher");
    }

    public String creator() {
        return env.getRequiredProperty("creator");
    }

    public String serverRoot() {
        return env.getRequiredProperty("serverRoot");
    }

    public boolean ezidRequests() {
        return env.getRequiredProperty("ezidRequests", boolean.class);
    }

    public boolean ignoreUser() {
        return env.getRequiredProperty("ignoreUser", boolean.class);
    }

    public String loginPageUrl() {
        return env.getRequiredProperty("loginPageUrl");
    }

    public int naan() {
        return env.getRequiredProperty("naan", int.class);
    }

    public String mailUser() {
        return env.getRequiredProperty("mailUser");
    }

    public String mailPassword() {
        return env.getRequiredProperty("mailPassword");
    }

    public String mailFrom() {
        return env.getRequiredProperty("mailFrom");
    }

    public boolean debug() {
        return env.getRequiredProperty("debug", boolean.class);
    }

    public String mapboxAccessToken() {
        return env.getRequiredProperty("mapboxAccessToken");
    }

    public String resolverTargetPrefix() {
        return env.getRequiredProperty("resolverTargetPrefix");
    }

    public String rights() {
        return env.getRequiredProperty("rights");
    }
}
