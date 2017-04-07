package biocode.fims.elasticSearch.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain object containing ElasticSearch query information
 */
public class ElasticSearchQuery {
    private static final Pageable DEFAULT_PAGE = new PageRequest(0, 10);

    private Pageable pageable = DEFAULT_PAGE;
    private String[] types;
    private String[] indicies;
    private List<String> source;
    private QueryBuilder query;

    public ElasticSearchQuery(QueryBuilder query, String[] indicies, String[] types) {
        this.query = query;
        this.indicies = indicies;
        this.types = types;
        this.source = new ArrayList<>();
    }

    public String[] getIndicies() {
        return indicies;
    }

    public QueryBuilder getQuery() {
        return query;
    }

    public Pageable getPageable() {
        return pageable;
    }

    public ElasticSearchQuery pageable(Pageable pageable) {
        this.pageable = pageable;
        return this;
    }

    public String[] getTypes() {
        return types;
    }

    public List<String> getSource() {
        return source;
    }

    public ElasticSearchQuery source(List<String> source) {
        this.source = source;
        return this;
    }

}
