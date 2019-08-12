package biocode.fims.application.config;

import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.records.FimsRowMapper;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.GenericRecordRowMapper;
import biocode.fims.records.Record;
import biocode.fims.reader.DataConverterFactory;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.reader.plugins.CSVReader;
import biocode.fims.reader.plugins.ExcelReader;
import biocode.fims.reader.plugins.TabReader;
import biocode.fims.repositories.*;
import biocode.fims.repositories.BcidRepository;
import biocode.fims.run.DatasetAuthorizer;
import biocode.fims.run.FimsDatasetAuthorizer;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.RecordValidatorFactory;
import biocode.fims.validation.ValidatorInstantiator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;
import javax.ws.rs.client.ClientBuilder;

/**
 * Configuration for resource needed for all biocode-fims applications. This includes web and cli apps
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"biocode.fims.service"})
@Import({SettingsManagerConfig.class, DataAccessConfig.class, MessageSourceConfig.class, FimsProperties.class})
public class FimsAppConfig {
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

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
    public DataConverterFactory dataConverterFactory() {
        return new DataConverterFactory(new HashMap<>());
    }

    @Bean
    public RecordValidatorFactory recordValidatorFactory() {
        Map<Class<? extends Record>, ValidatorInstantiator> validators = new HashMap<>();

        validators.put(GenericRecord.class, new RecordValidator.DefaultValidatorInstantiator());

        return new RecordValidatorFactory(validators);
    }

    @Bean
    public RecordRepository recordRepository(FimsProperties fimsProperties) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("record-repository-sql.yml"));

        Map<Class<? extends Record>, FimsRowMapper<? extends Record>> rowMappers = new HashMap<>();
        rowMappers.put(GenericRecord.class, new GenericRecordRowMapper());

        return new PostgresRecordRepository(jdbcTemplate, yaml.getObject(), rowMappers, fimsProperties);
    }

    @Bean
    public NetworkConfigRepository networkConfigRepository() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("network-config-repository-sql.yml"));

        return new PostgresNetworkConfigRepository(jdbcTemplate, yaml.getObject());
    }

    @Bean
    public ProjectAuthorizer projectAuthorizer() {
        return new ProjectAuthorizer(projectRepository);
    }


    @Bean
    public QueryAuthorizer queryAuthorizer(ProjectService projectService, ExpeditionService expeditionService) {
        return new QueryAuthorizer(projectService, expeditionService);
    }

    @Bean
    public BcidRepository bcidRepository(FimsProperties fimsProperties) {
        return new BcidRepository(ClientBuilder.newClient(), fimsProperties);
    }

    @Bean
    public PostgresRepositoryAuditAdvice postgresRepositoryAuditAdvice() {
        return new PostgresRepositoryAuditAdvice();
    }

    @Bean
    public FimsDatasetAuthorizer fimsDatasetAuthorizer(FimsProperties props, ExpeditionService expeditionService, ProjectService projectService) {
        return new FimsDatasetAuthorizer(props, expeditionService, projectService);
    }
}
