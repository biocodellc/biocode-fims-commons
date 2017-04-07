package biocode.fims.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * simple file cache used to cache files for later downloading. This is useful b/c XHR requests can't download file.
 * we can use the fileCache, then return a url where the user can later fetch the file with a GET request.
 * @author rjewing
 */
public class FileCache {
    Map<String, CachedFile> files;

    public FileCache() {
        this.files = new HashMap<>();
    }

    public void addFile(CachedFile file) {
        this.files.put(file.getId(), file);
    }

    public CachedFile getFile(String id, int userId) {
        CachedFile file = files.remove(id);

        if (file != null && file.authorizedUser(userId)) {
            return file;
        }

        return null;
    }
}
