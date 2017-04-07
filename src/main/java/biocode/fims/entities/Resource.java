package biocode.fims.entities;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ResourceCode;
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
    private ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;

        Resource resource1 = (Resource) o;

        if (getBcid() != null ? !getBcid().equals(resource1.getBcid()) : resource1.getBcid() != null) return false;
        return resource != null ? resource.equals(resource1.resource) : resource1.resource == null;
    }

    @Override
    public int hashCode() {
        int result = getBcid() != null ? getBcid().hashCode() : 0;
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        return result;
    }
}
