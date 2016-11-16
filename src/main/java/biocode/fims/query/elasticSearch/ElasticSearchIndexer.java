package biocode.fims.query.elasticSearch;

import biocode.fims.config.ConfigurationFileEsMapper;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.SendEmail;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.reindex.BulkIndexByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;

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
            JSONObject resource = (JSONObject) obj;
            String id = expeditionCode + "_" + resource.get(uniqueKey);
            // denormalize the resource. Adding the expeditionCode allows us to query resources by expeditionCode
            resource.put("expedition", expedition);

            bulkRequest.add(
                    client.prepareIndex(String.valueOf(projectId), TYPE, id)
                            .setSource(resource)
            );
        }

        BulkResponse response = bulkRequest.get();

        if (response.hasFailures()) {
            logger.error(response.buildFailureMessage());
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
            SendEmail sendEmail = new SendEmail(
                    settingsManager.retrieveValue("mailUser"),
                    settingsManager.retrieveValue("mailPassword"),
                    settingsManager.retrieveValue("mailFrom"),
                    settingsManager.retrieveValue("mailUser"),
                    "Error update elastic mapping - projectId [" + projectId + "]",
                    e.toString());
            sendEmail.start();
        }
    }
}
