package biocode.fims.application.config;

import biocode.fims.rest.versioning.VersionTransformer;
import org.springframework.context.annotation.*;

/**
 * configuration class for any biocode-fims implementation webapp
 */
@Configuration
@ComponentScan(basePackages = {"biocode.fims.rest"})
@EnableAspectJAutoProxy
public abstract class FimsWebAppConfig {

    @Bean
    public VersionTransformer versionTransformer() {
        return new VersionTransformer();
    }

}
