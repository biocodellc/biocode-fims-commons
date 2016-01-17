package biocode.fims.rest;

import biocode.fims.rest.filters.AdminFilter;
import biocode.fims.rest.filters.AuthenticatedFilter;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.server.ResourceConfig;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
/**
 * This class extends the jax-rs Application class and initializes the biocode.fims.settings.SettingsManager upon startup
 */
public class FimsApplication extends ResourceConfig {

    public FimsApplication(@Context ServletContext sc) {
        super();
        // retrieve the properties filename from the web.xml
        String filename = sc.getInitParameter("propsFilename");
        //initialize the settings manager
        SettingsManager.getInstance(filename);
        register(AuthenticatedFilter.class);
        register(AdminFilter.class);
    }
}
