package biocode.fims.rest;

import biocode.fims.rest.filters.AdminFilter;
import biocode.fims.rest.filters.AuthenticatedFilter;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
/**
 * This class extends the jax-rs Application class and initializes the biocode.fims.settings.SettingsManager upon startup
 */
public class FimsApplication extends ResourceConfig {

    public FimsApplication() {
        super();
        ApplicationContext rootCtx = ContextLoader.getCurrentWebApplicationContext();
        // retrieve the properties filename from the web.xml
        String filename = ((XmlWebApplicationContext) rootCtx).getServletContext().getInitParameter("propsFilename");
        //initialize the settings manager
        SettingsManager.getInstance(filename);

        register(FimsExceptionMapper.class);

        register(RequestContextFilter.class);
        register(AuthenticatedFilter.class);
        register(AdminFilter.class);
    }
}
