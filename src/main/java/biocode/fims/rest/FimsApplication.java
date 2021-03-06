package biocode.fims.rest;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.rest.filters.*;
import biocode.fims.rest.services.subResources.*;
import biocode.fims.utils.SpringApplicationContext;
import org.apache.commons.lang3.ArrayUtils;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
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
        register(new NetworkId.Binder());
        register(DynamicViewFilter.class);

        register(FimsExceptionMapper.class);

        register(RequestContextFilter.class);
        register(AuthenticatedFilter.class);
        register(AdminFilter.class);
        register(APIVersionFilter.class);
        register(AuthenticationFilter.class);
        register(AuthenticatedUserResourceFilter.class);
        register(RequestLoggingFilter.class);

        // need to manually register all subResources. This is so they get registered with the SpringComponentProvider
        // otherwise, the VersionTransformer advice will not register with the subResource method
        register(ProjectsResource.class);
        register(ProjectMembersResource.class);
        register(ProjectConfigResource.class);
        register(ProjectConfigurationResource.class);
        register(NetworksResource.class);
        register(NetworkConfigurationResource.class);
        register(ExpeditionsResource.class);
        register(ProjectTemplatesResource.class);
        register(RecordsResource.class);
    }

    /**
     * Checks that all REST resources registered under the biocode.fims.rest.services package are Spring beans.
     * If a REST resource is not managed by spring, the VersionTransformer will not recognize the REST resource method,
     * thus our requests/responses will not be transformed into/from the current REST version.
     */
    @PostConstruct
    public void resourceBeanCheck() {
        List<Class> nonSpringBeans = new ArrayList<>();
        List<Class> invalidScopedBeans = new ArrayList<>();

        for (Class resourceClass : getClasses()) {
            if (resourceClass.getPackage().getName().startsWith("biocode.fims.rest.services")) {

                // check for @Component annotation or for the bean in the applicationContext
                if (AnnotationUtils.findAnnotation(resourceClass, Component.class) == null
                        && SpringApplicationContext.getBean(resourceClass) == null) {
                    nonSpringBeans.add(resourceClass);
                }

                // by default jersey resources are request scoped, however spring @Components are
                // singleton scoped. If the resource is singleton scope, we need to mark it w/
                // @Singleton to enable more resource validation by Jersey
                if (!SpringApplicationContext.isPrototypeScopedBean(resourceClass) &&
                        AnnotationUtils.findAnnotation(resourceClass, Singleton.class) == null) {
                    invalidScopedBeans.add(resourceClass);
                }
            }
        }

        if (!nonSpringBeans.isEmpty()) {
            throw new FimsRuntimeException("Jersey Resource Class " + ArrayUtils.toString(nonSpringBeans) + " is not a" +
                    " spring bean. All resource classes must be annotated with either @Controller, " +
                    "@Component, @Service, or @Repository. This is to ensure that the " +
                    "VersionTransformer.class aop advice registers all REST resource methods", 500);
        }

        if (!invalidScopedBeans.isEmpty()) {
            throw new FimsRuntimeException("Jersey Resource Class " + ArrayUtils.toString(invalidScopedBeans) + " is not" +
                    " request scoped and is missing the @Singleton annotation. This is necessary to ensure that the " +
                    "Jersey performs the correct resource validation for non request-scoped beans (Jersey default).", 500);
        }
    }
}
