package biocode.fims.elasticSearch.query;

import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Domain object containing ElasticSearch query information
 */
public class ElasticSearchQuery {
    private static final Pageable DEFAULT_PAGE = new PageRequest(0, 10);

    private Pageable pageable = DEFAULT_PAGE;
    private String[] types;
    private String[] indicies;
    private String[] source;
    private QueryBuilder query;

    public ElasticSearchQuery(QueryBuilder query, String[] indicies) {
        this.query = query;
        this.indicies = indicies;
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

    public ElasticSearchQuery types(String[] types) {
        this.types = types;
        return this;
    }

    public String[] getSource() {
        return source;
    }

    public ElasticSearchQuery source(String[] source) {
        this.source = source;
        return this;
    }
}
