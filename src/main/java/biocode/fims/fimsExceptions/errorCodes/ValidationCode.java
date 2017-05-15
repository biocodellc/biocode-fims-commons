package biocode.fims.fimsExceptions.errorCodes;

/**
 * {@link ErrorCode} to handle validation errors
 */
public enum ValidationCode implements ErrorCode {
    EMPTY_DATASET,
    DUPLICATE_COLUMNS,
    INVALID_DATASET,
    NO_DATA
}
