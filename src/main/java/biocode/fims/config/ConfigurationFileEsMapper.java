package biocode.fims.config;

import biocode.fims.digester.*;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * class to convert a configuration file to a ElasticSearch Mapping object.
 * This produces a flat elasticSearch mapping to the config. When looping through the entities, if the entity contains
 * esNestedObject=true, then we will treat all attributes in that entity as a Nested object in elasticSearch.
 * Otherwise, the attributes are added to a single json object.
 */
public class ConfigurationFileEsMapper {

    private static final Map<DataType, String> DATA_TYPE_MAP;

    static {
        DATA_TYPE_MAP = new HashMap<>();
        DATA_TYPE_MAP.put(DataType.DATE, "date");
        DATA_TYPE_MAP.put(DataType.TIME, "date");
        DATA_TYPE_MAP.put(DataType.DATETIME, "date");
        DATA_TYPE_MAP.put(DataType.INTEGER, "integer");
        DATA_TYPE_MAP.put(DataType.FLOAT, "float");
        DATA_TYPE_MAP.put(DataType.STRING, "text");
    }

    public static JSONObject convert(File configFile) {
        JSONObject properties = new JSONObject();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        for (Entity entity : mapping.getEntities()) {

            if (entity.isEsNestedObject()) {
                JSONObject property = new JSONObject();
                JSONObject nestedObjectProperties = new JSONObject();

                for (Attribute attribute: entity.getAttributes()) {
                    nestedObjectProperties.put(attribute.getUri(), getAttributePropertyInfo(attribute));
                }

                property.put("type", "nested");
                property.put("properties", nestedObjectProperties);
                properties.put(entity.getConceptAlias(), property);
            } else {
                for (Attribute attribute: entity.getAttributes()) {
                    properties.put(attribute.getUri(), getAttributePropertyInfo(attribute));
                }
            }

        }

        JSONObject esMapping = new JSONObject();
        esMapping.put("properties", properties);

        return esMapping;
    }

    private static JSONObject getAttributePropertyInfo(Attribute a) {
        switch (DATA_TYPE_MAP.get(a.getDatatype())) {
            case "integer":
                return getIntegerPropertyInfo();
            case "float":
                return getFloatPropertyInfo();
            case "date":
                return getDatePropertyInfo(a);
            default:
                // default to text
                return getTextPropertyInfo();
        }
    }

    private static JSONObject getTextPropertyInfo() {
        JSONObject propertyInfo = new JSONObject();

        JSONObject fields = new JSONObject();

        JSONObject keywordField = new JSONObject();
        keywordField.put("type", "keyword");
        keywordField.put("ignore_above", 10922);
        fields.put("keyword", keywordField);


        propertyInfo.put("fields", fields);
        propertyInfo.put("type", "text");
        return propertyInfo;
    }

    private static JSONObject getIntegerPropertyInfo() {
        JSONObject propertyInfo = new JSONObject();
        propertyInfo.put("type", "integer");
        propertyInfo.put("ignore_malformed", true);
        return propertyInfo;
    }

    private static JSONObject getFloatPropertyInfo() {
        JSONObject propertyInfo = new JSONObject();
        propertyInfo.put("type", "float");
        propertyInfo.put("ignore_malformed", true);
        return propertyInfo;
    }

    private static JSONObject getDatePropertyInfo(Attribute attribute) {
        JSONObject propertyInfo = new JSONObject();
        propertyInfo.put("type", "date");
        propertyInfo.put("format", attribute.getDataformat().replaceAll(",", "||"));
        propertyInfo.put("ignore_malformed", true);
        return propertyInfo;
    }
}
