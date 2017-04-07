package biocode.fims.tools;

/**
 * @author rjewing
 */
public class CachedFile {
    private String id;
    private String path;
    private int userId;
    private String name;

    public CachedFile(String id, String path, int userId, String name) {
        this.id = id;
        this.path = path;
        this.userId = userId;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public boolean authorizedUser(int userId) {
        return !(this.userId > 0 && this.userId != userId);
    }
}
