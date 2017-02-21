package biocode.fims.entities;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ResourceCode;
import biocode.fims.rest.SpringObjectMapper;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rjewing
 */
public class Resource {

    private String bcid;
    private ObjectNode resource;
    private ObjectMapper objectMapper = new SpringObjectMapper();

    public Resource(String bcid, ObjectNode resource) {
        this.bcid = bcid;
        this.resource = resource;
    }

    public String getBcid() {
        return bcid;
    }

    public <T>T getProperty(String propertyPath, Class<T> returnType) {
        JsonNode node = resource.at(generateJsonPointer(propertyPath));

        if (node.isMissingNode()) {
            throw new FimsRuntimeException(ResourceCode.UNKNOWN_PROPERTY, 500);
        }

        try {
            return objectMapper.treeToValue(node, returnType);
        } catch (JsonProcessingException e) {
            throw new FimsRuntimeException(ResourceCode.INVALID_RETURN_TYPE, 500);
        }
    }

    private JsonPointer generateJsonPointer(String propertyPath) {
        if (!propertyPath.startsWith("/")) {
            propertyPath = "/" + propertyPath;
        }

        return JsonPointer.compile(propertyPath);
    }

    public void setProperty(String propertyPath, Object property) {
        resource.putPOJO(propertyPath, property);
    }

    public String asJsonString() {
        try {
            return objectMapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            throw new FimsRuntimeException(ResourceCode.INVALID_SERIALIZATION, 500);
        }
    }
}
