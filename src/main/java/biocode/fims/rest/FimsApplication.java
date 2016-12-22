package biocode.fims.rest;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.rest.filters.*;
import biocode.fims.rest.services.rest.subResources.*;
import biocode.fims.utils.SpringApplicationContext;
import org.apache.commons.lang.ArrayUtils;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

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
        register(AuthenticatedUserResourceFilter.class);

        // need to manually register an subResources. This is so they get registered with the SpringComponentProvider
        // otherwise, the VersionTransformer advice will not register with the subResource method
        register(ProjectResource.class);
        register(ProjectConfigurationResource.class);
        register(ProjectConfigurationListResource.class);
        register(ExpeditionsResource.class);
        register(UserProjectResource.class);
        register(UserProjectExpeditionsResource.class);
    }

    /**
     * Checks that all REST resources registered under the biocode.fims.rest.services package are Spring beans.
     * If a REST resource is not managed by spring, the VersionTransformer will not reconize the REST resource method,
     * thus our requests/responses will not be transformed into/from the current REST version.
     */
    @PostConstruct
    public void resourceBeanCheck() {
        List<Class> nonSpringBeans = new ArrayList<>();

        for (Class resourceClass : getClasses()) {
            if (resourceClass.getPackage().getName().startsWith("biocode.fims.rest.services")) {

                // check for @Component annotation or for the bean in the applicationContexgt
                if (AnnotationUtils.findAnnotation(resourceClass, Component.class) == null
                        && SpringApplicationContext.getBean(resourceClass) == null) {
                    nonSpringBeans.add(resourceClass);
                }
            }
        }

        if (!nonSpringBeans.isEmpty()) {
            throw new FimsRuntimeException("Jersey Resource Class " + ArrayUtils.toString(nonSpringBeans) + " is not a" +
                    " spring bean. All resource classes must be annotated with either @Controller, " +
                    "@Component, @Service, or @Repository. This is to ensure that the " +
                    "VersionTransformer.class aop advice registers all REST resource methods", 500);
        }
    }
}
