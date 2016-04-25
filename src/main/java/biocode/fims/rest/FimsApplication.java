package biocode.fims.rest;

import biocode.fims.rest.filters.AdminFilter;
import biocode.fims.rest.filters.AuthenticatedFilter;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * This class extends the jax-rs Application class and initializes the biocode.fims.settings.SettingsManager upon startup
 */
public class FimsApplication extends ResourceConfig {

    public FimsApplication() {
        super();

        register(ObjectMapperContextResolver.class);
        register(JacksonFeature.class);

        register(FimsExceptionMapper.class);

        register(RequestContextFilter.class);
        register(AuthenticatedFilter.class);
        register(AdminFilter.class);
    }
}
