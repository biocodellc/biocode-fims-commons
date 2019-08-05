package biocode.fims.repositories;

import biocode.fims.models.Project;
import biocode.fims.rest.responses.PaginatedResponse;
import biocode.fims.records.RecordSources;
import biocode.fims.config.models.Entity;
import biocode.fims.records.Record;
import biocode.fims.records.RecordResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.run.Dataset;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class TestRecordRepository implements RecordRepository {
    private List<RecordStore> stores = new ArrayList<>();

    @Override
    public RecordResult get(String rootIdentifier, String localIdentifier) {
        throw new NotImplementedException();
    }

    @Override
    public boolean delete(String rootIdentifier, String localIdentifier) {
        throw new NotImplementedException();
    }

    @Override
    public List<? extends Record> getRecords(Project project, String conceptAlias, Class<? extends Record> recordType) {
        return stores.stream()
                .filter(s -> s.projectId == project.getProjectId() && s.conceptAlias.equals(conceptAlias))
                .flatMap(s -> s.records.stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends Record> getRecords(Project project, String expeditionCode, String conceptAlias, List<String> localIdentifiers, Class<? extends Record> recordType) {
        throw new NotImplementedException();
    }

    @Override
    public List<? extends Record> getRecords(Project project, String expeditionCode, String conceptAlias, Class<? extends Record> recordType) {
        RecordStore ex = new RecordStore(project.getProjectId(), expeditionCode, conceptAlias);

        return stores.stream()
                .filter(s -> s.equals(ex))
                .findAny()
                .orElse(ex)
                .records;
    }

    @Override
    public void saveChildRecord(Record record, int projectId, Entity parentEntity, Entity entity) {
        throw new NotImplementedException();
    }

    @Override
    public void saveRecord(Record record, int projectId, Entity entity) {
        throw new NotImplementedException();
    }

    @Override
    public void saveDataset(Dataset dataset, int projectId) {
        throw new NotImplementedException();
    }

    @Override
    public <T> List<T> query(String sql, SqlParameterSource params, Class<T> responseType) {
        throw new NotImplementedException();
    }

    @Override
    public <T> List<T> query(String sql, SqlParameterSource params, RowMapper<T> rowMapper) {
        throw new NotImplementedException();
    }

    @Override
    public QueryResults query(Query query) {
        throw new NotImplementedException();
    }

    @Override
    public PaginatedResponse<Map<String, List<Map<String, Object>>>> query(Query query, RecordSources sources, boolean includeEmptyProperties) {
        throw new NotImplementedException();
    }

    public void addRecord(int projectId, String expeditionCode, String conceptAlias, Record record) {
        RecordStore store = stores.stream()
                .filter(s -> s.projectId == projectId
                        && s.expeditionCode.equals(expeditionCode)
                        && s.conceptAlias.equals(conceptAlias))
                .findFirst().orElse(null);

        if (store == null) {
            store = new RecordStore(projectId, expeditionCode, conceptAlias);
            stores.add(store);
        }

        store.records.add(record);
    }

    public void addRecords(int projectId, String expeditionCode, String conceptAlias, List<Record> records) {
        RecordStore store = stores.stream()
                .filter(s -> s.projectId == projectId
                        && s.expeditionCode.equals(expeditionCode)
                        && s.conceptAlias.equals(conceptAlias))
                .findFirst().orElse(null);

        if (store == null) {
            store = new RecordStore(projectId, expeditionCode, conceptAlias);
            stores.add(store);
        }

        store.records.addAll(records);
    }

    private static class RecordStore {
        private int projectId;
        private String expeditionCode;
        private String conceptAlias;
        private List<Record> records;

        private RecordStore(int projectId, String expeditionCode, String conceptAlias) {
            this.projectId = projectId;
            this.expeditionCode = expeditionCode;
            this.conceptAlias = conceptAlias;
            this.records = new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RecordStore)) return false;

            RecordStore that = (RecordStore) o;

            if (projectId != that.projectId) return false;
            if (!expeditionCode.equals(that.expeditionCode)) return false;
            return conceptAlias.equals(that.conceptAlias);
        }

        @Override
        public int hashCode() {
            int result = projectId;
            result = 31 * result + expeditionCode.hashCode();
            result = 31 * result + conceptAlias.hashCode();
            return result;
        }
    }
}
