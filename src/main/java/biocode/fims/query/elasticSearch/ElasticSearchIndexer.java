package biocode.fims.query.elasticSearch;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for sending JSON documents to be Elasticsearch for indexing
 */
public class ElasticSearchIndexer {
    private final Logger logger = LoggerFactory.getLogger(ElasticSearchIndexer.class);

    private final Client client;
    private final String TYPE = "dataset";

    public ElasticSearchIndexer(Client client) {
        this.client = client;
    }

    public void index(String index, String id, JSONObject document) {
        IndexResponse response = client.prepareIndex(index, TYPE, id)
                .setSource(document.toJSONString())
                .get();
    }

    public void bulkIndex(int projectId, String expeditionId, String uniqueKey, JSONArray dataset) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();

        for (Object obj : dataset) {
            JSONObject sample = (JSONObject) obj;
            String id = expeditionId + "_" + sample.get(uniqueKey);

            bulkRequest.add(
                    client.prepareIndex(String.valueOf(projectId).toLowerCase(), TYPE, id)
                            .setSource(sample)
            );
        }

        BulkResponse response = bulkRequest.get();

        if (response.hasFailures()) {
            logger.error(response.buildFailureMessage());
        }

    }
}
