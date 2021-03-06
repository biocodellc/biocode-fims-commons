package biocode.fims.query.writers;

import biocode.fims.config.models.DataType;
import com.fasterxml.jackson.core.JsonPointer;

/**
 * java bean to hold information for Transforming Json Fields into human readable output
 *
 * @author RJ Ewing
 */
public class JsonFieldTransform {

    private final JsonPointer path;
    private final DataType dataType;
    private final String fieldName;
    private final String uri;


    /**
     * @param fieldName the new name of the field
     * @param uri       the uri of the field
     * @param dataType  the {@link DataType} of the field
     */
    public JsonFieldTransform(String fieldName, String uri, DataType dataType, boolean childObject) {
        this.fieldName = fieldName;
        this.uri = uri;
        this.dataType = dataType;
        String pointerPath = "/" + uri.replaceAll("~", "~0")
                        .replaceAll("/", "~1");

        if (childObject) {
            pointerPath = pointerPath.replaceAll("\\.", "/");
        }

        this.path = JsonPointer.compile(pointerPath);
    }

    public String getFieldName() {
        return fieldName;
    }

    /**
     * path {@see https://tools.ietf.org/html/draft-ietf-appsawg-json-pointer-03#page-3} in the {@link com.fasterxml.jackson.databind.node.ObjectNode} for the field to transform
     */
    public JsonPointer getPath() {
        return path;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getUri() {
        return uri;
    }
}
