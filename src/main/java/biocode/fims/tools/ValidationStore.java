package biocode.fims.tools;

import biocode.fims.rest.responses.ValidationResponse;
import biocode.fims.run.DatasetProcessor;
import biocode.fims.run.ProcessorStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple store used for holding Validation status & results for async processing
 * <p>
 * Entries will expire after 2 hrs.
 *
 * @author rjewing
 */
public class ValidationStore {
    private final static int CACHE_EXPRIATION = 2 * 60 * 60 * 1000; // 2 hrs

    private final Map<UUID, ValidationResult> store;

    public ValidationStore() {
        store = new ConcurrentHashMap<>();
    }

    public UUID put(UUID id, ProcessorStatus status, int userId) {
        store.put(id, new ValidationResult(status, userId));
        removeExpired();

        return id;
    }

    public void update(UUID id, ValidationResponse response, Throwable throwable) {
        ValidationResult validationResult = store.get(id);
        if (validationResult != null) {
            validationResult.setResponse(response);
            validationResult.setException(throwable);
        }
    }

    public ValidationResult get(UUID id, int userId) {
        ValidationResult validationResult = store.get(id);

        if (validationResult != null && validationResult.authorizedUser(userId)) {
            return validationResult;
        }

        return null;
    }

    public void invalidate(UUID id) {
        store.remove(id);
    }

    private void removeExpired() {
        List<UUID> expired = new ArrayList<>();

        for (Map.Entry<UUID, ValidationResult> entry : store.entrySet()) {
            if (entry.getValue().isExpired()) {
                expired.add(entry.getKey());
            }
        }

        for (UUID id : expired) {
            store.remove(id);
        }
    }


    public static class ValidationResult {
        private final ProcessorStatus status;
        private ValidationResponse response;
        private final Date ts;
        private int userId;
        private Throwable exception;

        private ValidationResult(ProcessorStatus status, int userId) {
            this.status = status;
            this.userId = userId;
            this.ts = new Date();
        }

        public ProcessorStatus status() {
            return status;
        }

        public ValidationResponse response() {
            if (response != null && status != null) response.setStatus(status.statusHtml());
            return response;
        }

        public Throwable exception() {
            return exception;
        }

        private void setResponse(ValidationResponse response) {
            this.response = response;
        }

        private boolean isExpired() {
            return ts.getTime() < (new Date().getTime() - CACHE_EXPRIATION);
        }

        private boolean authorizedUser(int userId) {
            return !(this.userId > 0 && this.userId != userId);
        }

        public void setException(Throwable exception) {
            this.exception = exception;
        }
    }

}

