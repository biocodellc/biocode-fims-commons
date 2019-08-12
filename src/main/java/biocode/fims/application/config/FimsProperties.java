package biocode.fims.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * @author rjewing
 */
@Component
public class FimsProperties {
    protected final ConfigurableEnvironment env;

    @Autowired
    public FimsProperties(ConfigurableEnvironment env) {
        this.env = env;
    }

    public String appRoot() {
        return env.getRequiredProperty("appRoot");
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
        return getRequiredRawProperty("accountCreatePath");
    }

    public String resetPasswordPath() {
        return getRequiredRawProperty("resetPasswordPath");
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

    public String bcidResolverPrefix() {
        String prefix = env.getProperty("bcid.resolverPrefix", "https://n2t.net/");
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix;
    }

    public URI entityResolverTarget() {
        return env.getRequiredProperty("bcid.resolverTargets.entity", URI.class);
    }

    public URI expeditionResolverTarget() {
        return env.getRequiredProperty("bcid.resolverTargets.expedition", URI.class);
    }

    /**
     * Return the raw string property value.
     * <p>
     * By default Spring will attempt to resolve placeholders within a property value
     * <p>
     * ex. prop1=some/${value}
     * <p>
     * An attempt will be made to replace ${value} with a property "value". If the
     * resolving fails, then an exception will throw.
     * <p>
     * This function avoids that funny business
     *
     * @param key property name to resolve
     * @return
     */
    private String getRequiredRawProperty(String key) {
        for (PropertySource source : env.getPropertySources()) {
            String val = (String) source.getProperty(key);
            if (val != null) return val;
        }

        throw new IllegalStateException("Failed to resolve property: " + key);
    }
}
