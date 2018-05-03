package biocode.fims.rest.responses;

import biocode.fims.validation.messages.EntityMessages;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * @author rjewing
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ValidationResponse {
    /**
     * @exclude
     */
    private UUID processId;
    private Boolean isValid;
    private Boolean hasError;
    private String exception;
    private List<EntityMessages> messages;
    private String uploadUrl;
    private String status;

    public ValidationResponse() {
    }

    /**
     * used for asynchronous validations
     *
     * @param id
     */
    public ValidationResponse(UUID id) {
        this.processId = id;
    }

    /**
     * Used for synchronous validations
     *
     * @param id
     * @param isValid
     * @param hasError
     * @param messages
     * @param uploadUrl
     */
    public ValidationResponse(UUID id, boolean isValid, boolean hasError, List<EntityMessages> messages, String uploadUrl) {
        this.processId = id;
        this.isValid = isValid;
        this.hasError = hasError;
        this.messages = messages;
        this.uploadUrl = uploadUrl;
    }

    /**
     * id for the validation process.
     * <p>
     * Used to continue the upload after validating. If the validation was called with waitForCompletion = false, this id is used to fetch the validation status & results
     */
    public UUID getId() {
        return processId;
    }

    /**
     * Is this a valid dataset. Will be false if any warnings or errors occured
     */
    @JsonProperty("isValid")
    public Boolean isValid() {
        return isValid;
    }

    /**
     * did any errors or exceptions occur during validation
     */
    @JsonProperty()
    public Boolean hasError() {
        return hasError;
    }

    public List<EntityMessages> getMessages() {
        return messages;
    }

    /**
     * api endpoint to upload the validated dataset
     */
    public String getUploadUrl() {
        return uploadUrl;
    }

    /**
     * current status of the validation
     */
    public String getStatus() {
        return status;
    }

    /**
     * The exception that occurred during validation.
     */
    public String getException() {
        return exception;
    }

    public static ValidationResponse withStatus(String status) {
        ValidationResponse response = new ValidationResponse();
        response.status = status;
        return response;
    }

    public static ValidationResponse withException(String msg) {
        ValidationResponse response = new ValidationResponse();
        response.exception = msg;
        response.isValid = false;
        response.hasError = true;
        return response;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

