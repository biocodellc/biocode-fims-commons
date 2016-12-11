package biocode.fims.rest;

import biocode.fims.rest.filters.APIVersionFilter;
import biocode.fims.rest.filters.AdminFilter;
import biocode.fims.rest.filters.AuthenticatedFilter;
import biocode.fims.rest.filters.AuthenticationFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

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
        register(APIVersionFilter.class);
        register(AuthenticationFilter.class);
    }
}
