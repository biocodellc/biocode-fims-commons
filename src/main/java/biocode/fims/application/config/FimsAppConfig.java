package biocode.fims.application.config;

import biocode.fims.bcid.Resolver;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.reader.plugins.CSVReader;
import biocode.fims.reader.plugins.ExcelReader;
import biocode.fims.reader.plugins.TabReader;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.SpringApplicationContext;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.RecordValidatorFactory;
import biocode.fims.validation.ValidatorInstantiator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public DataReaderFactory dataReaderFactory() {
        Map<DataReader.DataReaderType, List<DataReader>> dataReaders = new HashMap<>();

        dataReaders.put(
                TabularDataReaderType.READER_TYPE,
                Arrays.asList(new CSVReader(), new TabReader(), new ExcelReader())
        );
        return new DataReaderFactory(dataReaders);
    }

    @Bean
    public RecordValidatorFactory recordValidatorFactory() {
        Map<Class<? extends Record>, ValidatorInstantiator> validators = new HashMap<>();

        validators.put(GenericRecord.class, new RecordValidator.DefaultValidatorInstantiator());

        return new RecordValidatorFactory(validators);
    }
}
