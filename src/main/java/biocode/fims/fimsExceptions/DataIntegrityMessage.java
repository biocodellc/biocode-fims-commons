package biocode.fims.fimsExceptions;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * @author rjewing
 */
public class DataIntegrityMessage {
    private final DataIntegrityViolationException exception;

    public DataIntegrityMessage(DataIntegrityViolationException e) {
        this.exception = e;
    }

    @Override
    public String toString() {
        return exception.getCause().getCause().getMessage().replaceFirst("\".*\"", "");
    }
}
