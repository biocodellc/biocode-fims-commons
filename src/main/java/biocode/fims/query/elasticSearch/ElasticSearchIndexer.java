package biocode.fims.query.elasticSearch;

import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.EmailUtils;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.BulkIndexByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
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
    private final static String TYPE = "resource";

    public ElasticSearchIndexer(Client client) {
        this.client = client;
    }

    /**
     * method for indexing an entire dataset. We expect each resource in the dataset to have a "bcid" field,
     * as we use the bcid field for the document id
     *
     * @param projectId
     * @param expeditionCode
     * @param dataset
     */
    public void indexDataset(int projectId, String expeditionCode, JSONArray dataset) {
        BulkRequestBuilder bulkRequest = client.prepareBulk();

        // TODO don't delete until after successfully indexing?
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
            JSONObject resource = (JSONObject) obj;

            bulkRequest.add(
                    client.prepareIndex(String.valueOf(projectId), TYPE, String.valueOf(resource.get("bcid")))
                            .setSource(resource)
            );
        }

        BulkResponse response = bulkRequest.get();

        if (response.hasFailures()) {
            logger.error(response.buildFailureMessage());
            EmailUtils.sendAdminEmail(
                    "ElasticSearch error indexing dataset",
                    response.buildFailureMessage()
            );
        }

    }

    public void updateMapping(int projectId, JSONObject mapping, SettingsManager settingsManager) {
        String index = String.valueOf(projectId);

        try {
            IndicesExistsResponse response = client.admin().indices().prepareExists(index).get();

            if (!response.isExists()) {

                // if the index doesn't exist yet, then we need to create it
                client.admin().indices().prepareCreate(index)
                        .addMapping(TYPE, mapping).get();

            } else {

                client.admin().indices().preparePutMapping(index)
                        .setType(TYPE)
                        .setSource(mapping).get();

            }
        } catch (Exception e) {
            logger.warn("", e);
            EmailUtils.sendAdminEmail(
                    "Error update elastic mapping - projectId [" + projectId + "]",
                    e.toString()
            );
        }
    }
}
