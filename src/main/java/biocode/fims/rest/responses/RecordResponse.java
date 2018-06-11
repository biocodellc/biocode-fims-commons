package biocode.fims.rest.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * @author RJ Ewing
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordResponse {
    public final Map<String, String> parent;
    public final Map<String, String> record;
    public final List<Map<String, String>> children;

    public RecordResponse(Map<String, String> parent, Map<String, String> record, List<Map<String, String>> children) {
        this.parent = parent;
        this.record = record;
        this.children = children;
    }
}