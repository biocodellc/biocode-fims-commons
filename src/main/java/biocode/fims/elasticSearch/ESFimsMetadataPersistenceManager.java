package biocode.fims.elasticSearch;

import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import biocode.fims.run.ProcessController;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.simple.JSONArray;

/**
 * {@link FimsMetadataPersistenceManager} for Elastic Search
 */
public class ESFimsMetadataPersistenceManager implements FimsMetadataPersistenceManager {
    private final Client client;

    public ESFimsMetadataPersistenceManager(Client client) {
        this.client = client;
    }

    @Override
    public void upload(ProcessController processController, JSONArray fimsMetadata) {
        // do nothing. all elasticsearch "uploading" is handled in FimsMetadataFileManager.index
    }

    @Override
    public boolean validate(ProcessController processController) {
        return true;
    }

    @Override
    public String getWebAddress() {
        return null;
    }

    @Override
    public String getGraph() {
        return null;
    }

    @Override
    public JSONArray getDataset(ProcessController processController) {
        SearchResponse response = client.prepareSearch(String.valueOf(processController.getProjectId()))
                .setQuery(QueryBuilders.matchQuery("expedition.expeditionCode", processController.getExpeditionCode()))
                .get();

        JSONArray dataset = new JSONArray();

        response.getHits().forEach(hit -> dataset.add(hit.source()));
        return new JSONArray();
    }
}
