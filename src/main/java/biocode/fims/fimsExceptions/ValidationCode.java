package biocode.fims.fimsExceptions;

/**
 * {@link ErrorCode} to handle validation errors
 */
public enum ValidationCode implements ErrorCode {
    EMPTY_DATASET,
    DUPLICATE_COLUMNS,
    INVALID_SHEET,
    NO_DATA
}
