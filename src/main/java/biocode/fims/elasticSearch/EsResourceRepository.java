package biocode.fims.elasticSearch;

import biocode.fims.entities.Resource;
import biocode.fims.repositories.ResourceRepository;
import biocode.fims.utils.EmailUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author rjewing
 */
public abstract class EsResourceRepository implements ResourceRepository {
    private final static Logger logger = LoggerFactory.getLogger(EsResourceRepository.class);

    private Client esClient;

    public EsResourceRepository(Client esClient) {
        this.esClient = esClient;
    }

    @Override
    public void save(List<Resource> resources, int projectId) {
        BulkRequestBuilder bulkRequest = esClient.prepareBulk();

        try {
            for (Resource resource: resources) {
                bulkRequest.add(
                        esClient.prepareIndex(String.valueOf(projectId), ElasticSearchIndexer.TYPE, resource.getBcid())
                                .setSource(resource.asJsonString())
                );
            }

            BulkResponse response = bulkRequest.get();

            if (response.hasFailures()) {
                logger.error(response.buildFailureMessage());
                EmailUtils.sendAdminEmail(
                        "ElasticSearch error updating resources with SRA accession numbers",
                        response.buildFailureMessage()
                );
            }
        } catch (Exception e) {
            logger.error("", e);
            EmailUtils.sendAdminEmail(
                    "ElasticSearch error updating resources with SRA accession numbers",
                    ExceptionUtils.getFullStackTrace(e)
            );

        }
    }
}
