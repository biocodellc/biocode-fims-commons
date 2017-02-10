package biocode.fims.api.services;

/**
 * @author rjewing
 */
public interface Request<T> {

    T execute();
}
