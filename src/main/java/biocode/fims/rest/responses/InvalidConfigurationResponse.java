package biocode.fims.rest.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author rjewing
 */
public class InvalidConfigurationResponse {
    @JsonProperty
    private List<String> errors;

    public InvalidConfigurationResponse(List<String> errors) {
        this.errors = errors;
    }
}
