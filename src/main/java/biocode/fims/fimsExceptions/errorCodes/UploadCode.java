package biocode.fims.fimsExceptions.errorCodes;

/**
 * {@link ErrorCode} to handle uploading errors
 */
public enum UploadCode implements ErrorCode {
    USER_NO_OWN_EXPEDITION,
    INVALID_EXPEDITION,
    EXPEDITION_CREATE,
    DATA_SERIALIZATION_ERROR
}
