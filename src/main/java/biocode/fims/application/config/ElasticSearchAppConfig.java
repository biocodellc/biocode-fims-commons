package biocode.fims.application.config;

import biocode.fims.elasticSearch.TransportClientFactoryBean;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for Fims Applications using ElasticSearch
 */
@Configuration
public class ElasticSearchAppConfig {
    @Autowired
    private Environment env;
    @Bean
    // This bean handles the creation/destruction of the esClient bean that is autowired
    public TransportClientFactoryBean transportClientFactoryBean() {
        TransportClientFactoryBean factoryBean = new TransportClientFactoryBean();
        factoryBean.setClusterName(env.getProperty("clusterName"));
        factoryBean.setClientIgnoreClusterName(Boolean.valueOf(env.getProperty("clientIgnoreClusterName")));
        factoryBean.setClientNodesSamplerInterval(env.getProperty("clientNodesSamplerInterval"));
        factoryBean.setClientPingTimeout(env.getProperty("clientPingTimeout"));
        factoryBean.setClientTransportSniff(Boolean.valueOf(env.getProperty("clientTransportSniff")));
        factoryBean.setClusterNodes(env.getProperty("clusterNodes"));
        return factoryBean;
    }
}
