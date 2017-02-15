package biocode.fims.api.services;

/**
 * @author rjewing
 */
public interface PaginatedRequest<T> extends Request<T> {

    T getMoreResults();
}
