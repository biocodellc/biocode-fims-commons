package biocode.fims.query.elasticSearch;

/*
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;


import static org.elasticsearch.client.RestClientBuilder.DEFAULT_MAX_RETRY_TIMEOUT_MILLIS;

// TODO use this when the elasticSearch java REST client becomes more mature
public class RestClientFactoryBean implements FactoryBean<RestClient>, InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(RestClientFactoryBean.class);

    private String[] hostnames = new String[] {"127.0.0.1:9200"};
    private RestClient client;
    private int maxRetryTimeoutMillis = DEFAULT_MAX_RETRY_TIMEOUT_MILLIS;
    private String pathPrefix = "/";


    @Override
    public RestClient getObject() throws Exception {
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return RestClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private void buildClient() {
        HttpHost[] hosts = new HttpHost[hostnames.length];
        for (int i = 0; i < hosts.length; i++) {
            hosts[i] = HttpHost.create(hostnames[i]);
        }

        client = RestClient.builder(hosts)
                .setMaxRetryTimeoutMillis(maxRetryTimeoutMillis)
                .setPathPrefix(pathPrefix)
                .build();
    }

    @Override
    public void destroy() throws Exception {
        try {
            logger.info("Closing elasticSearch client");
            if (client != null) {
                client.close();
            }
        } catch (final Exception e) {
            logger.error("Error closing ElasticSearch client: ", e);
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        buildClient();
    }

    public void setHostnames(String[] hostnames) {
        this.hostnames = hostnames;
    }

    public String[] getHostnames() {
        return hostnames;
    }

    public int getMaxRetryTimeoutMillis() {
        return maxRetryTimeoutMillis;
    }

    public void setMaxRetryTimeoutMillis(int maxRetryTimeoutMillis) {
        this.maxRetryTimeoutMillis = maxRetryTimeoutMillis;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }
}
*/