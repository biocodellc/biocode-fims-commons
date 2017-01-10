package biocode.fims.elasticSearch.query;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.rest.SpringObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * class to query elasticsearch
 * TODO create a query interface for fims
 */
public class ElasticSearchQuerier {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchQuerier.class);
    private static final TimeValue DEFAULT_TIME_VALUE = new TimeValue(1, TimeUnit.MINUTES);

    private final Client client;
    private final ElasticSearchQuery query;

    public ElasticSearchQuerier(Client client, ElasticSearchQuery query) {
        this.client = client;
        this.query = query;
    }


    public Page<ObjectNode> getPageableResults() {
        ObjectMapper objectMapper = new SpringObjectMapper();
        SearchRequestBuilder searchRequestBuilder = getPageableSearchRequestBuilder();
        SearchResponse response = getResponse(searchRequestBuilder);

        List<ObjectNode> results = new ArrayList<>();
        long totalHits = response.getHits().getTotalHits();

        try {
            for (SearchHit hit : response.getHits()) {
                results.add(objectMapper.readValue(hit.source(), ObjectNode.class));
            }
        } catch (IOException e) {
            throw new FimsRuntimeException(GenericErrorCode.SERVER_ERROR, "failed to convert query hit to ObjectNode", 500);
        }

        return new PageImpl<>(results, query.getPageable(), totalHits);

    }

    public ArrayNode getAllResults() {
        ObjectMapper objectMapper = new SpringObjectMapper();
        ArrayNode resources = objectMapper.createArrayNode();

        SearchRequestBuilder searchRequestBuilder = getScrollableSearchRequestBuilder();
        SearchResponse response = getResponse(searchRequestBuilder);

        do {
            for (SearchHit hit : response.getHits()) {
                resources.add(objectMapper.valueToTree(hit.sourceAsMap()));
            }

            response = getResponse(client.prepareSearchScroll(response.getScrollId()).setScroll(DEFAULT_TIME_VALUE));
        } while (response.getHits().getHits().length != 0);

        return resources;
    }

    private SearchResponse getResponse(ActionRequestBuilder searchRequestBuilder) {
        SearchResponse response = (SearchResponse) searchRequestBuilder.get();

        if (!(response.status() == RestStatus.OK || response.status() == RestStatus.NO_CONTENT)) {
            throw new ServerErrorException("Server Error fetching resources", response.toString());
        }

        return response;
    }

    private SearchRequestBuilder getPageableSearchRequestBuilder() {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder();

        searchRequestBuilder.setFrom(query.getPageable().getPageNumber() * query.getPageable().getPageSize());
        searchRequestBuilder.setSize(query.getPageable().getPageSize());

        return searchRequestBuilder;
    }

    private SearchRequestBuilder getScrollableSearchRequestBuilder() {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder();

        searchRequestBuilder.setScroll(DEFAULT_TIME_VALUE);
        searchRequestBuilder.setSize(1000);

        return searchRequestBuilder;
    }

    private SearchRequestBuilder getSearchRequestBuilder() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(query.getIndicies());

        searchRequestBuilder.setTypes(query.getTypes());

        searchRequestBuilder.setQuery(query.getQuery());
        searchRequestBuilder.setFetchSource(query.getSource(), null);

        return searchRequestBuilder;
    }
}
