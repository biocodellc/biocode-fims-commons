package biocode.fims.rest.versioning;

import javax.ws.rs.core.MultivaluedMap;
import java.util.LinkedHashMap;

/**
 * This is the specification for transforming requests and responses for jersey Resources. The Transformer implemention
 * is looked up via the {@link VersionTransformer}. Transformer implementations are looked for in the package:
 * "biocode.fims.rest.versioning.transformers", and are to be named {ResourceClassSimpleName}Transformer{APIVersion}
 */
public interface Transformer {

    void updateRequestData(LinkedHashMap<String, Object> argMap, String methodName, MultivaluedMap<String, String> queryParameters);

    /**
     * transform the returnValue from APIVersion +1 to this APIVersion returnVal. Note it is not possible
     * to modify the return type, only the value.
     * @param returnVal The response from the resource method with the APIVersion 1 greater then this transformer
     * @param methodName the name of the resource method to transform the response for
     * @return
     */
    Object updateResponseData(Object returnVal, String methodName);
}
