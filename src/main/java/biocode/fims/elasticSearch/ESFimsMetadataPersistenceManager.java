package biocode.fims.elasticSearch;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.digester.Attribute;
import biocode.fims.fileManagers.fimsMetadata.AbstractFimsMetadataPersistenceManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataPersistenceManager;
import biocode.fims.rest.SpringObjectMapper;
import biocode.fims.run.ProcessController;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * {@link FimsMetadataPersistenceManager} for Elastic Search
 */
public class ESFimsMetadataPersistenceManager extends AbstractFimsMetadataPersistenceManager implements FimsMetadataPersistenceManager {
    private final Client client;
    private String graph;

    public ESFimsMetadataPersistenceManager(Client client, FimsProperties props) {
        super(props);
        this.client = client;
    }

    @Override
    public void upload(ProcessController processController, ArrayNode fimsMetadata, String filename) {
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
        // doing this until we remove the deprecated ProjectRestService.getLatestGraphsByExpedition
        // this maintains backwards compatibility with v1 query api
        return graph == null ? graph = String.valueOf(UUID.randomUUID()) : graph;
    }

    @Override
    public ArrayNode getDataset(ProcessController processController) {
        List<Attribute> attributes = processController.getMapping().getDefaultSheetAttributes();
        SearchResponse response = client.prepareSearch(String.valueOf(processController.getProjectId()))
                .setTypes(ElasticSearchIndexer.TYPE)
                .setScroll(new TimeValue(1, TimeUnit.MINUTES))
                .setQuery(QueryBuilders.matchQuery("expedition.expeditionCode.keyword", processController.getExpeditionCode()))
                .setSize(1000)
                .get();

        ArrayNode dataset = new SpringObjectMapper().createArrayNode();

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

    @Override
    public void deleteDataset(ProcessController processController) {
        ElasticSearchIndexer indexer = new ElasticSearchIndexer(client);
        indexer.deleteDataset(processController.getProjectId(), processController.getExpeditionCode());
    }
}
