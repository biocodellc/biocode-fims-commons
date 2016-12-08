package biocode.fims.application.config;

import biocode.fims.rest.versioning.VersionTransformer;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * configuration class for any biocode-fims implementation webapp.
 */
@Configuration
@ComponentScan(basePackages = {"biocode.fims.rest", VersionTransformer.TRANSFORMER_PACKAGE})
@EnableScheduling
@EnableAspectJAutoProxy
public class FimsWebAppConfig {

    @Bean
    public VersionTransformer versionTransformer() {
        return new VersionTransformer();
    }

}
