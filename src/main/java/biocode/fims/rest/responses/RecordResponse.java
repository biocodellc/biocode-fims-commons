package biocode.fims.rest.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * @author RJ Ewing
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordResponse {
    public final int projectId;
    public final Map<String, Object> parent;
    public final Map<String, Object> record;
    public final List<Map<String, Object>> children;

    public RecordResponse(int projectId, Map<String, Object> parent, Map<String, Object> record, List<Map<String, Object>> children) {
        this.projectId = projectId;
        this.parent = parent;
        this.record = record;
        this.children = children;
    }
}
