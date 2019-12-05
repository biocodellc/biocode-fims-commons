package biocode.fims.rest.models;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple store that is used to hold objects between REST requests.
 * <p>
 * Entries will expire 5 mins after creation
 *
 * @author rjewing
 */
public class UploadStore<T> {
    private final static int CACHE_EXPRIATION = 5 * 60 * 1000; // 5 mins
    protected final Map<UUID, UploadMetadata> store;

    public UploadStore() {
        store = new ConcurrentHashMap<>();
    }

    public UUID put(UUID id, T obj, int userId) {
//        TODO is CACHE_EXPIRATION overridden? If so, migrate ValidationStore to use this
        store.put(id, new UploadMetadata(obj, userId));
        removeExpired();

        return id;
    }

    public T get(UUID id, int userId) {
        UploadMetadata metadata = store.get(id);

        if (metadata != null && metadata.authorizedUser(userId)) {
            return metadata.obj;
        }

        return null;
    }

    public void invalidate(UUID id) {
        store.remove(id);
    }

    private void removeExpired() {
        List<UUID> expired = new ArrayList<>();

        for (Map.Entry<UUID, UploadMetadata> entry : store.entrySet()) {
            if (entry.getValue().isExpired(getCacheExpiration())) {
                expired.add(entry.getKey());
            }
        }

        for (UUID id : expired) {
            store.remove(id);
        }
    }

    protected int getCacheExpiration() {
        return CACHE_EXPRIATION;
    }

    private class UploadMetadata {
        private final T obj;
        private final Date ts;
        private int userId;

        private UploadMetadata(T obj, int userId) {
            this.obj = obj;
            this.userId = userId;
            this.ts = new Date();
        }

        private boolean isExpired(int expiration) {
            return ts.getTime() < (new Date().getTime() - expiration);
        }

        public boolean authorizedUser(int userId) {
            return !(this.userId > 0 && this.userId != userId);
        }
    }
}
