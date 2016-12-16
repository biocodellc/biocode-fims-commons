package biocode.fims.elasticSearch;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Mapping;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class ElasticSearchUtils {

    /**
     * map the elasticsource hit source to a JSONObject, transforming each attribute uri -> column
     * @param source
     * @return
     */
    public static JSONObject transformResource(Map<String, Object> source, List<Attribute> attributes) {
        Mapping mapping = new Mapping();
        JSONObject transformedSource = new JSONObject();

        for (String key: source.keySet()) {

            String transformedKey = mapping.lookupColumnForUri(key, attributes);

            if (transformedKey == null) {
                transformedKey = key;
            }

            transformedSource.put(
                    transformedKey,
                    source.get(key)
            );

        }

        return transformedSource;
    }
}
