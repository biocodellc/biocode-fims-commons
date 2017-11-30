package biocode.fims.application.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author rjewing
 */
@Configuration
@EnableJpaRepositories(basePackages = "biocode.fims.repositories")
//@ComponentScan(basePackages = {"biocode.fims.models", "biocode.fims.entities"})
@Import({SettingsManagerConfig.class, MessageSourceConfig.class})
@PropertySource("classpath:biocode-fims-database.properties")
@EnableTransactionManagement
public class DataAccessConfig {
    @Autowired
    private Environment env;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource d = new DriverManagerDataSource(env.getProperty("bcidUrl"), env.getProperty("bcidUser"),
                env.getProperty("bcidPassword"));

        d.setDriverClassName("org.postgresql.Driver");

        return d;
    }

    @Bean
    public NamedParameterJdbcTemplate jdbcTemplate() {
        return new NamedParameterJdbcTemplate(dataSource());
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean f = new LocalContainerEntityManagerFactoryBean();

        Properties jpaProperties = new Properties();
        jpaProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL94Dialect");
        jpaProperties.setProperty("hibernate.show_sql", env.getProperty("hibernate.show_sql"));
        jpaProperties.setProperty("hibernate.formate_sql", env.getProperty("hibernate.format_sql"));
        jpaProperties.setProperty("hibernate.id.new_generator_mappings", "true");
        jpaProperties.setProperty("hibernate.max_fetch_depth", "3");

        f.setDataSource(dataSource());
        f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        f.setPackagesToScan("biocode.fims.models", "biocode.fims.entities");// TODO remove entities when removing BcidTmp
        f.setMappingResources("sql-results-mappings.xml", "named-queries.xml");
        f.setJpaProperties(jpaProperties);

        return f;
    }

    @Bean
    public JpaTransactionManager transactionManager() {
        return new JpaTransactionManager(entityManagerFactory().getObject());
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslationPostProcessor() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
