package biocode.fims.tools;

import biocode.fims.models.User;
import biocode.fims.utils.StringGenerator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * simple file cache used to cache files for later downloading. This is useful b/c XHR requests can't download file.
 * we can use the fileCache, then return a url where the user can later fetch the file with a GET request.
 *
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

    /**
     * Add a file to the cache
     *
     * @param file the file to cache
     * @param user (optional) if null, CachedFile userId is set to 0
     * @param name name of the CachedFile
     * @return id of the cached file
     */
    public String cacheFileForUser(File file, User user, String name) {
        int userId = user == null ? 0 : user.getUserId();
        String fileId = StringGenerator.generateString(20);
        CachedFile cf = new CachedFile(fileId, file.getAbsolutePath(), userId, name);
        addFile(cf);
        return fileId;
    }

    /**
     * Add a file to the cache
     *
     * @param file the file to cache
     * @param user (optional) if null, CachedFile userId is set to 0
     * @return id of the cached file
     */
    public String cacheFileForUser(File file, User user) {
        return cacheFileForUser(file, user, file.getName());
    }

    public CachedFile getFile(String id, int userId) {
        CachedFile file = files.remove(id);

        if (file != null && file.authorizedUser(userId)) {
            return file;
        }

        return null;
    }
}
