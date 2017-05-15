package biocode.fims.application.config;

import biocode.fims.rest.UserContext;
import biocode.fims.rest.versioning.VersionTransformer;
import biocode.fims.rest.versioning.VersionUrlConfig;
import biocode.fims.tools.FileCache;
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

    @Bean
    @Scope(value="request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public UserContext userContext() {
        return new UserContext();
    }

    @Bean
    public FileCache fileCache() {
        return new FileCache();
    }
}
