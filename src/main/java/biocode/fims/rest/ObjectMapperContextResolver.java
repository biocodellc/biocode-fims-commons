package biocode.fims.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Context Resolver for Jackson object mapper
 */
@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

    @Autowired
    private SpringObjectMapper objectMapper;

    public ObjectMapperContextResolver() {
        super();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }

}
