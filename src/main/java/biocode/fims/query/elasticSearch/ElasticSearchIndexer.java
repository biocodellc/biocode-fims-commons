package biocode.fims.query.elasticSearch;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.BulkIndexByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
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
    private final static String TYPE = "sample";

    public ElasticSearchIndexer(Client client) {
        this.client = client;
    }

    /**
     * method for indexing an entire dataset
     *
     * @param projectId
     * @param expeditionCode
     * @param uniqueKey
     * @param dataset
     */
    public void indexDataset(int projectId, String expeditionCode, String uniqueKey, JSONArray dataset) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        JSONObject expedition = new JSONObject();
        expedition.put("expeditionCode", expeditionCode);

        BulkIndexByScrollResponse deleteResponse = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
                .source(String.valueOf(projectId))
                .filter(QueryBuilders.termQuery("_type", TYPE))
                .filter(
                        QueryBuilders.matchQuery("expedition.expeditionCode", expeditionCode)
                ).execute().actionGet();

        if (deleteResponse.getBulkFailures().size() > 0) {
            logger.error("Error deleting previous indexes for expedition: {} and projectId: {}", expeditionCode, projectId);
            for (BulkItemResponse.Failure failure : deleteResponse.getBulkFailures()) {
                logger.error("document id: {}   message: {}", failure.getId(), failure.getMessage());
            }
            logger.error("Expedition dataset index may be out of sync. Old samples may be present.");
        }

        for (Object obj : dataset) {
            JSONObject sample = (JSONObject) obj;
            String id = expeditionCode + "_" + sample.get(uniqueKey);
            // denormalize the sample. Adding the expeditionCode allows us to query samples by expeditionCode
            sample.put("expedition", expedition);

            bulkRequest.add(
                    client.prepareIndex(String.valueOf(projectId), TYPE, id)
                            .setSource(sample)
            );
        }

        BulkResponse response = bulkRequest.get();

        if (response.hasFailures()) {
            logger.error(response.buildFailureMessage());
        }

    }
}
