package biocode.fims.query.writers;

import java.io.File;
import java.util.List;

/**
 * @author RJ Ewing
 */
public interface QueryWriter {

    List<File> write();
}
