package biocode.fims.application.config;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;


/**
 * Configuration for SettingManager
 */
@Configuration
@PropertySource("classpath:settings-manager.properties")
public class SettingsManagerConfig {
    @Autowired
    private Environment env;

    @Bean
    public SettingsManager settingsManager() {
        Resource propsFileResource = new ClassPathResource(env.getRequiredProperty("propsFile"));
        if (!propsFileResource.exists()) {
            throw new FimsRuntimeException(env.getRequiredProperty("propsFile") + " File not found", 500);
        }
        return SettingsManager.getInstance(propsFileResource);
    }
}
