package biocode.fims.elasticSearch.query;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * class to query elasticsearch
 * TODO create a query interface for fims
 */
public class EsQuery {

    private final Client client;

    public EsQuery(Client client) {
        this.client = client;
    }

    /*
    public Page<JSONObject> query(int projectId, List<String> expeditionCodes, Map<String, String> filters, Pageable pageable) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(projectId, expeditionCodes, filters, null);

        searchRequestBuilder.setFrom(pageable.getPageNumber() * pageable.getPageSize());
        searchRequestBuilder.setSize(pageable.getPageSize());

        SearchResponse response = searchRequestBuilder.get();

        if (response.status() != RestStatus.OK || response.status() != RestStatus.NO_CONTENT) {
            throw new ServerErrorException("Server Error fetching resources", response.toString());
        }

        return mapResults(response, pageable);

    }
    */

    public JSONArray query(int projectId, List<String> expeditionCodes, Map<String, String> filters, Mapping mapping) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(projectId, expeditionCodes, filters, mapping);

        searchRequestBuilder.setSize(1000);
        SearchResponse response = searchRequestBuilder.get();

        if (!(response.status() == RestStatus.OK || response.status() == RestStatus.NO_CONTENT)) {
            throw new ServerErrorException("Server Error fetching resources", response.toString());
        }

        return mapResults(response);
    }

    private SearchRequestBuilder getSearchRequestBuilder(int projectId, List<String> expeditionCodes, Map<String, String> filters, Mapping mapping) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(String.valueOf(projectId));

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        expeditionCodes.forEach(e -> {
            boolQueryBuilder.should(QueryBuilders.matchQuery("expedition.expeditionCode", e));
        });

        List<Attribute> attributes = mapping.getDefaultSheetAttributes();
        filters.forEach((field, value) -> {
            boolQueryBuilder.must(QueryBuilders.matchQuery(getColumnFromUri(attributes, field), value));
        });

        searchRequestBuilder.setQuery(boolQueryBuilder);

        return searchRequestBuilder;
    }

    // TODO remove this in favor of mapping.lookup*
    private String getColumnFromUri(List<Attribute> attributes, String uri) {
        // assume this is a column if it doesn't contain a semi-colon
        if (!uri.contains(":")) {
            return  uri;
        }
        for (Attribute attribute: attributes) {
            if (StringUtils.equals(attribute.getUri(), uri)) {
                return attribute.getColumn();
            }
        }
        return null;
    }

    private Page<JSONObject> mapResults(SearchResponse response, Pageable pageable) {
        List<JSONObject> results = new ArrayList<>();
        long totalHits = response.getHits().getTotalHits();

        for (SearchHit hit: response.getHits()) {
            if (!StringUtils.isBlank(hit.getSourceAsString())) {
                results.add(new JSONObject(hit.getSource()));
            }
        }

        return new PageImpl<>(results, pageable, totalHits);
    }

    private JSONArray mapResults(SearchResponse response) {
        JSONArray results = new JSONArray();

        for (SearchHit hit: response.getHits()) {
            if (!StringUtils.isBlank(hit.getSourceAsString())) {
                results.add(new JSONObject(hit.getSource()));
            }
        }

        return results;
    }
}
