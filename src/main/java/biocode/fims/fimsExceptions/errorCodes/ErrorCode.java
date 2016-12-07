package biocode.fims.fimsExceptions.errorCodes;

import biocode.fims.fimsExceptions.FimsAbstractException;

/**
 * Interface for application errors. Implementations are ment to be enums. We then can add the {@link ErrorCode}
 * to {@link FimsAbstractException}. This allows us to have less exception classes and to provide specific error
 * for exception handling, as well as localized messages.
 */
public interface ErrorCode {
}
