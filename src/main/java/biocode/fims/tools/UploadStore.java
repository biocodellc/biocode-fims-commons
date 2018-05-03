package biocode.fims.tools;

import biocode.fims.run.DatasetProcessor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple store used for holding UploadMetadata object between REST requests.
 * <p>
 * This is used between the validation and upload steps. Entries will expire after 5 mins.
 *
 * @author rjewing
 */
public class UploadStore {
    private final static int CACHE_EXPRIATION = 5 * 60 * 1000; // 5 mins

    private final Map<UUID, UploadMetadata> store;

    public UploadStore() {
        store = new ConcurrentHashMap<>();
    }

    public UUID put(UUID id, DatasetProcessor processor, int userId) {
        store.put(id, new UploadMetadata(processor, userId));
        removeExpired();

        return id;
    }

    public DatasetProcessor get(UUID id, int userId) {
        UploadMetadata metadata = store.get(id);

        if (metadata != null && metadata.authorizedUser(userId)) {
            return metadata.datasetProcessor;
        }

        return null;
    }

    public void invalidate(UUID id) {
        store.remove(id);
    }

    private void removeExpired() {
        List<UUID> expired = new ArrayList<>();

        for (Map.Entry<UUID, UploadMetadata> entry : store.entrySet()) {
            if (entry.getValue().isExpired()) {
                expired.add(entry.getKey());
            }
        }

        for (UUID id : expired) {
            store.remove(id);
        }
    }


    private static class UploadMetadata {
        private final DatasetProcessor datasetProcessor;
        private final Date ts;
        private int userId;

        private UploadMetadata(DatasetProcessor datasetProcessor, int userId) {
            this.datasetProcessor = datasetProcessor;
            this.userId = userId;
            this.ts = new Date();
        }

        private boolean isExpired() {
            return ts.getTime() < (new Date().getTime() - CACHE_EXPRIATION);
        }

        public boolean authorizedUser(int userId) {
            return !(this.userId > 0 && this.userId != userId);
        }
    }

}

