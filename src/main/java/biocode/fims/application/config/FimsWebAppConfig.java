package biocode.fims.application.config;

import biocode.fims.rest.versioning.VersionTransformer;
import biocode.fims.rest.versioning.VersionUrlConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

/**
 * configuration class for any biocode-fims implementation webapp.
 */
@Configuration
@ComponentScan(basePackages = {"biocode.fims.rest", VersionTransformer.TRANSFORMER_PACKAGE})
@EnableScheduling
@EnableAspectJAutoProxy
public class FimsWebAppConfig {

    @Value("classpath:api-version-urls.yaml")
    private Resource urlFile;


    @Bean
    public VersionTransformer versionTransformer() {
        return new VersionTransformer();
    }

    @Bean
    public VersionUrlConfig versionUrlConfig() throws IOException {
        if (urlFile.exists()) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(urlFile.getFile(), VersionUrlConfig.class);
        }

        return new VersionUrlConfig();
    }
}
