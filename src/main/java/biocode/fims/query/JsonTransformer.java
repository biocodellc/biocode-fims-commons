package biocode.fims.query;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * @author RJ Ewing
 */
public class JsonTransformer {

    /**
     * transforms the each field in the resource using the {@link JsonFieldTransform} list
     * @param resource
     * @param fieldTransforms
     * @return
     */
    public static ObjectNode transform(ObjectNode resource, List<JsonFieldTransform> fieldTransforms) {
        ObjectNode transformedResource = resource.objectNode();

        for (JsonFieldTransform field: fieldTransforms) {
            transformedResource.set(field.getFieldName(), resource.at(field.getPath()));
        }

        return transformedResource;

    }
}
