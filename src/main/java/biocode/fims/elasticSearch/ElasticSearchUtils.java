package biocode.fims.elasticSearch;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Mapping;
import biocode.fims.rest.SpringObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

public class ElasticSearchUtils {

    /**
     * map the elasticsource hit source to a ObjectNode, transforming each attribute uri -> column
     * @param source
     * @return
     */
    public static ObjectNode transformResource(Map<String, Object> source, List<Attribute> attributes) {
        Mapping mapping = new Mapping();
        ObjectMapper objectMapper = new SpringObjectMapper();
        ObjectNode transformedSource = objectMapper.createObjectNode();

        for (String key: source.keySet()) {

            String transformedKey = mapping.lookupColumnForUri(key, attributes);

            if (transformedKey == null) {
                transformedKey = key;
            }

            transformedSource.set(
                    transformedKey,
                    objectMapper.valueToTree(source.get(key))
            );

        }

        return transformedSource;
    }
}
