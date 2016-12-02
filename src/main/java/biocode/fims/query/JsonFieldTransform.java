package biocode.fims.query;

import biocode.fims.digester.DataType;
import com.fasterxml.jackson.core.JsonPointer;

/**
 * java bean to hold information for Transforming Json Fields into human readable output
 * @author RJ Ewing
 */
public class JsonFieldTransform {

    private final JsonPointer path;
    private final DataType dataType;
    private final String fieldName;


    /**
     * @param fieldName the new name of the field
     * @param path       the path {@see https://tools.ietf.org/html/draft-ietf-appsawg-json-pointer-03#page-3} in the {@link com.fasterxml.jackson.databind.node.ObjectNode} for the field to transform
     * @param dataType   the {@link DataType} of the field
     */
    public JsonFieldTransform(String fieldName, JsonPointer path, DataType dataType) {
        this.fieldName = fieldName;
        this.path = path;
        this.dataType = dataType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public JsonPointer getPath() {
        return path;
    }

    public DataType getDataType() {
        return dataType;
    }
}
