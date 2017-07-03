package biocode.fims.application.config;

import biocode.fims.bcid.Resolver;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;

import java.io.FileNotFoundException;

/**
 * Configuration for resource needed for all biocode-fims applications. This includes web and cli apps
 */
@Configuration
@ComponentScan(basePackages = {"biocode.fims.service"})
@Import({SettingsManagerConfig.class})
@ImportResource({
        "classpath:data-access-config.xml"
})
public class FimsAppConfig {
    @Autowired
    Environment env;

    @Autowired
    BcidService bcidService;
    @Autowired
    ExpeditionService expeditionService;
    @Autowired
    SettingsManager settingsManager;
    FimsProperties props;

    @Bean
    public Resolver resolver() throws FileNotFoundException {
        return new Resolver(bcidService, settingsManager, expeditionService);
    }

    @Bean
    public SpringApplicationContext springApplicationContext() {
        return new SpringApplicationContext();
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:locale/messages");
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Bean
    public FimsProperties fimsProperties() {
        return new FimsProperties(env);
    }
}
