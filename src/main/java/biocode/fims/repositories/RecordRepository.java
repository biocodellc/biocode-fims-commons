package biocode.fims.repositories;

import biocode.fims.digester.Attribute;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.query.QueryResult;
import biocode.fims.query.dsl.Query;
import biocode.fims.run.Dataset;

import java.util.List;

/**
 * @author rjewing
 */
public interface RecordRepository {

    List<? extends Record> getRecords(int projectId, String expeditionCode, String conceptAlias, Class<? extends Record> recordType);

    void save(Dataset dataset, int projectId, int expeditionId);

    void createEntityTable(int projectId, String conceptAlias);

    void createEntityTable(int projectId, String conceptAlias, List<String> indexedColumnUris);

    /**
     * @param projectId
     * @param conceptAlias
     * @param parentConceptAlias
     * @param parentReferenceColumn {@link Attribute} uri for column which references the parent {@link biocode.fims.digester.Entity}
     */
    void createChildEntityTable(int projectId, String conceptAlias, String parentConceptAlias, String parentReferenceColumn);

    void createChildEntityTable(int projectId, String conceptAlias, String parentConceptAlias, String parentReferenceColumn, List<String> indexedColumnUris);

    void createEntityTableIndex(int projectId, String conceptAlias, String column);

    void createProjectSchema(int projectId);

    QueryResult query(Query query);
}
