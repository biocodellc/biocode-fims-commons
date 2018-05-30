package biocode.fims.rest;

import biocode.fims.application.config.SettingsManagerConfig;
import biocode.fims.rest.filters.*;
import biocode.fims.rest.services.subResources.*;
import biocode.fims.rest.versioning.VersionTransformer;
import biocode.fims.rest.versioning.VersionUrlConfig;
import biocode.fims.tools.FileCache;
import biocode.fims.utils.SpringApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author rjewing
 */
@Configuration
@Import({SettingsManagerConfig.class})
@ComponentScan(basePackages = {"biocode.fims.service", "biocode.fims.rest", VersionTransformer.TRANSFORMER_PACKAGE})
@ImportResource({
        "classpath:test-data-access-config.xml"
})
@EnableAspectJAutoProxy
public class FimsRestTestAppConfig {
    @Value("classpath:api-version-urls.yaml")
    private Resource urlFile;

    @Bean
    public VersionTransformer versionTransformer() {
        return new VersionTransformer();
    }

    @Bean
    public UserContext userContext() {
        return new UserContext();
    }

    @Bean
    public VersionUrlConfig versionUrlConfig() throws IOException {
        if (urlFile.exists()) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(urlFile.getFile(), VersionUrlConfig.class);
        }

        return new VersionUrlConfig();
    }

    @Bean
    public FileCache fileCache() {
        return new FileCache();
    }

    @Bean
    public WebTargetFactoryBean webTargetFactoryBean() {
        WebTargetFactoryBean b = new WebTargetFactoryBean();

        b.setComponentClasses(new ArrayList<>(Arrays.asList(
                ObjectMapperContextResolver.class,
                JacksonFeature.class,

                FimsExceptionMapper.class,

                RequestContextFilter.class,
                AuthenticatedFilter.class,
                AdminFilter.class,
                APIVersionFilter.class,
                AuthenticationFilter.class,
                AuthenticatedUserResourceFilter.class,

                // need to manually register all subResources. This is so they get registered with the SpringComponentProvider
                // otherwise, the VersionTransformer advice will not register with the subResource method
                ProjectsResource.class,
                ProjectMembersResource.class,
                ProjectConfigurationResource.class,
                ExpeditionsResource.class,
                ProjectTemplatesResource.class
        )));

        return b;
    }

    @Bean
    public SpringApplicationContext springApplicationContext() {
        return new SpringApplicationContext();
    }
}
