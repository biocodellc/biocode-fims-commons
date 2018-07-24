package biocode.fims.rest.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * @author RJ Ewing
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedResponse<T> {
    public final int page;
    public final int limit;
    public final T content;

    public PaginatedResponse(T content, int page, int limit) {
        this.content = content;
        this.page = page;
        this.limit = limit;
    }
}
