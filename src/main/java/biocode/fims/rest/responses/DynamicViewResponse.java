package biocode.fims.rest.responses;

/**
 * Response wrapper which enables dynamically setting the {@link com.fasterxml.jackson.annotation.JsonView}
 * for serialization.
 * <p>
 * Useful if you would like to return a different view for authenticated vs un-authenticated users.
 *
 * @author rjewing
 */
public class DynamicViewResponse<T> {

    private final T entity;
    private final Class view;

    public DynamicViewResponse(T entity, Class view) {
        this.entity = entity;
        this.view = view;
    }

    public T getEntity() {
        return entity;
    }

    public Class getView() {
        return view;
    }
}
