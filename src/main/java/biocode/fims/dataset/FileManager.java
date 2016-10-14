package biocode.fims.dataset;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rjewing on 6/10/16.
 */
public interface FileManager {
    void validate(String outputFolder);
    void upload();
    void close();
    String[] getExtensions();

    class FileManagerMap {
        private final Map<String, Class<FileManager>> fileManagerMap = new HashMap<String, Class<FileManager>>() {{
            put("txt", DatasetFileManager.class);
            put("csv", DatasetFileManager.class);
            put("xls", DatasetFileManager.class);
            put("xlsx", DatasetFileManager.class);
        }};
    }
}
