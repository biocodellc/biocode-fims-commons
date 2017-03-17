package biocode.fims.query.dsl;

import biocode.fims.elasticSearch.FieldColumnTransformer;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class QueryClause implements ExpeditionQueryContainer, QueryExpression {
    protected final List<String> expeditions;
    List<QueryExpression> expressions;
    private FieldColumnTransformer transformer;
    private String column;

    public QueryClause() {
        expressions = new ArrayList<>();
        this.expeditions = new ArrayList();
    }

    public QueryClause(List<QueryExpression> expressions) {
        this.expressions = expressions;
        this.expeditions = new ArrayList();
    }

    public void add(QueryExpression e) {
        this.expressions.add(e);
    }

    @Override
    public List<QueryBuilder> getQueryBuilders() {
        if (needToSetQueryClauseColumn()) {
            setColumnsForQueryExpressions();
        }
        return expressions
                .stream()
                .flatMap(e -> e.getQueryBuilders().stream())
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryClause)) return false;

        QueryClause that = (QueryClause) o;

        if (!getExpeditions().equals(that.getExpeditions())) return false;
        if (!expressions.equals(that.expressions)) return false;
        if (transformer != null ? !transformer.equals(that.transformer) : that.transformer != null) return false;
        return column != null ? column.equals(that.column) : that.column == null;
    }

    @Override
    public int hashCode() {
        int result = getExpeditions().hashCode();
        result = 31 * result + expressions.hashCode();
        result = 31 * result + (transformer != null ? transformer.hashCode() : 0);
        result = 31 * result + (column != null ? column.hashCode() : 0);
        return result;
    }

    @Override
    public void setColumn(FieldColumnTransformer transformer, String column) {
        this.transformer = transformer;
        this.column = column;
    }

    private void setColumnsForQueryExpressions() {
        expressions.forEach(e -> e.setColumn(transformer, column));
    }

    private boolean needToSetQueryClauseColumn() {
        return transformer != null && !StringUtils.isBlank(column);
    }

    @Override
    public void addExpedition(String expeditionCode) {
        this.expeditions.add(expeditionCode);
    }

    public List<String> getExpeditions() {
        return expeditions;
    }
}
