package biocode.fims.application.config;

import biocode.fims.rest.versioning.VersionTransformer;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * configuration class for any biocode-fims implementation webapp.
 * Currently this class is ment to be extened, Thus you would need to copy all the annotations to the
 * subclass. TODO: Fix the problem with circular dependencies and use the @Import notation instead of subclassing
 */
@Configuration
@ComponentScan(basePackages = {"biocode.fims.rest"})
@EnableScheduling
@EnableAspectJAutoProxy
public abstract class FimsWebAppConfig {

    @Bean
    public VersionTransformer versionTransformer() {
        return new VersionTransformer();
    }

}
