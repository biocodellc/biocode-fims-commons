package biocode.fims.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * @author rjewing
 */
//@Primary
@Component
public class FimsProperties {
    protected final Environment env;

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
        return env.getRequiredProperty("bcid.publisher");
    }

    public String creator() {
        return env.getProperty("bcid.creator");
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

    public String accountCreatePath() {
        return env.getRequiredProperty("accountCreatePath");
    }

    public String resetPasswordPath() {
        return env.getRequiredProperty("resetPasswordPath");
    }

    public String bcidUrl() {
        return env.getRequiredProperty("bcid.url");
    }

    public String bcidClientId() {
        return env.getRequiredProperty("bcid.clientId");
    }

    public String bcidClientSecret() {
        return env.getRequiredProperty("bcid.clientSecret");
    }

    public URI entityResolverTarget() {
        return env.getRequiredProperty("bcid.resolverTargets.entity", URI.class);
    }

    public URI expeditionResolverTarget() {
        return env.getRequiredProperty("bcid.resolverTargets.expedition", URI.class);
    }
}
