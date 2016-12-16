package biocode.fims.rest;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.rest.filters.*;
import biocode.fims.rest.services.rest.subResources.ExpeditionsResource;
import biocode.fims.rest.services.rest.subResources.UserProjectExpeditionsResource;
import biocode.fims.rest.services.rest.subResources.UserProjectResource;
import org.apache.commons.lang.ArrayUtils;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * This class extends the jax-rs Application class and initializes the biocode.fims.settings.SettingsManager upon startup
 */
public class FimsApplication extends ResourceConfig {

    private static List<Class> springBeanAnnotations;

    static {
        springBeanAnnotations = new ArrayList<>();
        springBeanAnnotations.add(Controller.class);
        springBeanAnnotations.add(Component.class);
        springBeanAnnotations.add(Service.class);
        springBeanAnnotations.add(Repository.class);
    }

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

                boolean springBean = false;

                for (Annotation annotation : resourceClass.getDeclaredAnnotations()) {

                    if (springBeanAnnotations.contains(annotation.annotationType())) {
                        springBean = true;
                    }
                }

                if (!springBean) {
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
