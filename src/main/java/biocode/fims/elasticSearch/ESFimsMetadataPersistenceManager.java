package biocode.fims.elasticSearch;

import biocode.fims.digester.Attribute;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import biocode.fims.run.ProcessController;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
        List<Attribute> attributes = processController.getMapping().getDefaultSheetAttributes();
        SearchResponse response = client.prepareSearch(String.valueOf(processController.getProjectId()))
                .setTypes(ElasticSearchIndexer.TYPE)
                .setScroll(new TimeValue(1, TimeUnit.MINUTES))
                .setQuery(QueryBuilders.matchQuery("expedition.expeditionCode", processController.getExpeditionCode()))
                .setSize(1000)
                .get();

        JSONArray dataset = new JSONArray();

        do {
            for (SearchHit hit: response.getHits()) {
                dataset.add(
                        ElasticSearchUtils.transformResource(hit.getSource(), attributes)
                );
            }

            response = client.prepareSearchScroll(response.getScrollId()).setScroll(new TimeValue(1, TimeUnit.MINUTES)).get();
        } while (response.getHits().getHits().length != 0);

        return dataset;
    }
}
