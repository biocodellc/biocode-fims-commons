package biocode.fims.elasticSearch;

import biocode.fims.rest.SpringObjectMapper;
import biocode.fims.utils.EmailUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.elasticsearch.action.admin.indices.alias.Alias;
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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class for sending JSON documents to be Elasticsearch for indexing
 */
public class ElasticSearchIndexer {
    private final Logger logger = LoggerFactory.getLogger(ElasticSearchIndexer.class);

    private final Client client;
    public final static String TYPE = "resource";

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

        try {
            BulkResponse response = bulkRequest.get();

            if (response.hasFailures()) {
                logger.error(response.buildFailureMessage());
                EmailUtils.sendAdminEmail(
                        "ElasticSearch error indexing dataset",
                        response.buildFailureMessage()
                );
            }
        } catch (Exception e) {
            logger.error("", e);
            EmailUtils.sendAdminEmail(
                    "ElasticSearch error indexing dataset",
                    ExceptionUtils.getFullStackTrace(e)
            );

        }


    }

    public void updateMapping(int projectId, JSONObject mapping) {
        String indexAlias = String.valueOf(projectId);
        String index = indexAlias + "_" + new SimpleDateFormat("yyyyMMdd").format(new Date());

        try {
            IndicesExistsResponse response = client.admin().indices().prepareExists(indexAlias).get();

            if (!response.isExists()) {

                // if the index doesn't exist yet, then we need to create it
                client.admin().indices().prepareCreate(index)
                        .addMapping(TYPE, mapping)
                        .addAlias(new Alias(indexAlias))
                        .get();

            } else {

                client.admin().indices().preparePutMapping(index)
                        .setType(TYPE)
                        .setSource(mapping).get();

                EmailUtils.sendAdminEmail(
                        "ElasticSearch index mapping updated",
                        "ProjectId [" + projectId + "] configuration file was changed, resulting in a update to the corresponding " +
                                "ElasticSearch index. Existing data may need to be reindexed to reflect the changes.");

            }
        } catch (Exception e) {
            logger.warn("", e);
            EmailUtils.sendAdminEmail(
                    "Error update elastic mapping - projectId [" + projectId + "]",
                    ExceptionUtils.getFullStackTrace(e)
            );
        }
    }
}
