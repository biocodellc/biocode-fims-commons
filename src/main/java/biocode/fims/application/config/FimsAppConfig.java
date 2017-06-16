package biocode.fims.application.config;

import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.bcid.Resolver;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.GenericRecordRowMapper;
import biocode.fims.models.records.Record;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.reader.plugins.CSVReader;
import biocode.fims.reader.plugins.ExcelReader;
import biocode.fims.reader.plugins.TabReader;
import biocode.fims.repositories.*;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.SpringApplicationContext;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.RecordValidatorFactory;
import biocode.fims.validation.ValidatorInstantiator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for resource needed for all biocode-fims applications. This includes web and cli apps
 */
@Configuration
@EnableAspectJAutoProxy
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
    ProjectRepository projectRepository;
    @Autowired
    SettingsManager settingsManager;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    ExpeditionService expeditionService;

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

    @Bean
    public RecordRepository recordRepository() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("record-repository-sql.yml"));

        Map<Class<? extends Record>, RowMapper<? extends Record>> rowMappers = new HashMap<>();
        rowMappers.put(GenericRecord.class, new GenericRecordRowMapper());

        return new PostgresRecordRepository(jdbcTemplate, yaml.getObject(), rowMappers);
    }

    @Bean
    public PostgresRepositoryAuditAdvice recordRepositoryAdvice() {
        return new PostgresRepositoryAuditAdvice();
    }

    @Bean
    public ProjectConfigRepository projectConfigRepository() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("project-config-repository-sql.yml"));

        return new PostgresProjectConfigRepository(jdbcTemplate, yaml.getObject(),
                settingsManager, expeditionService);
    }

    @Bean
    public ProjectAuthorizer projectAuthorizer() {
        String appRoot = settingsManager.retrieveValue("appRoot");
        return new ProjectAuthorizer(projectRepository, appRoot);
    }
}
