package biocode.fims.fimsExceptions.errorCodes;

import java.io.File;

/**
 * {@link ErrorCode} to handle {@link File} operation errors
 *
 * @author RJ Ewing
 */
public enum FileCode implements ErrorCode {
    READ_ERROR, WRITE_ERROR
}
