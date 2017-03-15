package biocode.fims.query.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class Query {

    private StringBuilder queryString;
    private List<String> exists;
    private List<QueryFilter> filters;
    private List<Query> must;
    private List<Query> mustNot;
    private List<Query> should;
    private List<String> expeditions;

    public Query() {
        this.queryString = new StringBuilder();
        this.exists = new ArrayList<>();
        this.filters = new ArrayList<>();
        this.should= new ArrayList<>();
        this.must = new ArrayList<>();
        this.mustNot = new ArrayList<>();
        this.expeditions = new ArrayList<>();
    }

    public void addExists(String column) {
        exists.add(column);
    }

    public void addFilter(QueryFilter filter) {
        filters.add(filter);
    }

    public void addMust(Query mustQuery) {
        must.add(mustQuery);
    }

    public void addMustNot(Query mustNotQuery) {
        mustNot.add(mustNotQuery);
    }

    public void addShould(Query subQuery) {
        this.should.add(subQuery);
    }

    public void appendQueryString(String q) {
        queryString.append(" " + q);
    }

    public void addExpedition(String expeditionCode) {
        this.expeditions.add(expeditionCode);
    }

    public List<String> getExists() {
        return exists;
    }

    public String getQueryString() {
        return queryString.toString().trim();
    }

    public List<Query> getMust() {
        return must;
    }

    public List<String> getExpeditions() {
        return expeditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Query)) return false;

        Query query = (Query) o;

        if (!getQueryString().equals(query.getQueryString())) return false;
        if (!getExists().equals(query.getExists())) return false;
        if (!filters.equals(query.filters)) return false;
        if (!getMust().equals(query.getMust())) return false;
        if (!mustNot.equals(query.mustNot)) return false;
        if (!should.equals(query.should)) return false;
        return getExpeditions().equals(query.getExpeditions());
    }

    @Override
    public int hashCode() {
        int result = getQueryString().hashCode();
        result = 31 * result + getExists().hashCode();
        result = 31 * result + filters.hashCode();
        result = 31 * result + getMust().hashCode();
        result = 31 * result + mustNot.hashCode();
        result = 31 * result + should.hashCode();
        result = 31 * result + getExpeditions().hashCode();
        return result;
    }
}
