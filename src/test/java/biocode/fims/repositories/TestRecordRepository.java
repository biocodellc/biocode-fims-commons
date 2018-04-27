package biocode.fims.repositories;

import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.run.Dataset;
import org.springframework.data.domain.Page;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public List<? extends Record> getRecords(int projectId, String expeditionCode, String conceptAlias, Class<? extends Record> recordType) {
        RecordStore ex = new RecordStore(projectId, expeditionCode, conceptAlias);

        return stores.stream()
                .filter(s -> s.equals(ex))
                .findAny()
                .orElse(ex)
                .records;
    }

    @Override
    public void save(Dataset dataset, int projectId, int expeditionId) {
        throw new NotImplementedException();
    }

    @Override
    public QueryResults query(Query query) {
        throw new NotImplementedException();
    }

    @Override
    public Page<Map<String, String>> query(Query query, int page, int limit, List<String> source, boolean includeEmptyProperties) {
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
