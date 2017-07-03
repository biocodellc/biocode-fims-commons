package biocode.fims.api.services;

import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Map;

/**
 * Abstract class to facilitate making http requests to external APIs
 *
 * @author rjewing
 */
public class AbstractRequest<T> implements Request<T> {
    private final Class<T> responseClass;
    private String path;
    private final String baseUrl;
    private Client client;
    private String method;
    private Map<String, Object[]> queryParams;
    private MediaType accepts;
    private Entity httpEntity;
    private MultivaluedMap<String, Object> headers;

    public AbstractRequest(String method, Class<T> responseClass, Client client, String path, String baseUrl) {
        this.method = method;
        this.responseClass = responseClass;
        this.client = client;
        this.path = path;
        this.baseUrl = baseUrl;
        this.headers = new MultivaluedHashMap<>();
    }

    private void registerDefaultClientFeatures() {
        Configuration config = client.getConfiguration();

        if (!config.isRegistered(JacksonFeature.class)) {
            client.register(JacksonFeature.class);
        }
    }

    @Override
    public T execute() {
        registerDefaultClientFeatures();

        WebTarget target = client.target(baseUrl)
                .path(path);

        target = setQueryParamsOnTarget(target);

        return target.request()
                .accept(accepts)
                .headers(headers)
                .method(method, httpEntity, responseClass);
    }

    private WebTarget setQueryParamsOnTarget(WebTarget target) {
        for (Map.Entry<String, Object[]> entry : queryParams.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
        }

        return target;
    }

    public void addQueryParam(String key, Object... value) {
        queryParams.put(key, value);
    }

    protected void setQueryParams(Map<String, Object[]> queryParams) {
        this.queryParams = queryParams;
    }

    protected void setHttpEntity(Entity entity) {
        this.httpEntity = entity;
    }

    protected void setAccepts(MediaType accepts) {
        this.accepts = accepts;
    }

    public void addHeader(String name, String val) {
        this.headers.putSingle(name, val);
    }
}
