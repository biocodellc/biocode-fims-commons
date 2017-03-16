package biocode.fims.query;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class DeprecatedQuery {
    private List<QueryCriteria> criterion;
    private List<String> expeditions;
    private List<String> source;

    private DeprecatedQuery() {
        this.expeditions = new ArrayList<>();
        this.source = new ArrayList<>();
        this.criterion = new ArrayList<>();
    }

    public DeprecatedQuery(List<QueryCriteria> criterion, List<String> expeditions) {
        this.criterion = criterion;
        this.expeditions = expeditions;
        this.source = new ArrayList<>();
    }

    public List<QueryCriteria> getCriterion() {
        return criterion;
    }

    public List<String> getExpeditions() {
        return expeditions;
    }

    public List<String> getSource() {
        return source;
    }
}
