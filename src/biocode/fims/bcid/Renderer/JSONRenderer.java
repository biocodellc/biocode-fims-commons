package biocode.fims.bcid.Renderer;

import biocode.fims.bcid.*;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.SettingsManager;
import org.json.simple.JSONObject;

import java.lang.reflect.Field;

/**
 * jsonRenderer renders objects as JSON
 */
public class JSONRenderer extends Renderer {
    private JSONObject json;
    private Integer userId = null;
    private Resolver resolver = null;
    static SettingsManager sm;

    static {
        sm = SettingsManager.getInstance();
    }

    /**
     * constructor for displaying private dataset information
     * @param username
     */
    public JSONRenderer(String username, Resolver resolver, Bcid bcid) {
        super(bcid);
        Database db = new Database();
        userId = db.getUserId(username);
        this.resolver = resolver;
    }

    public JSONRenderer(Bcid bcid) {
        super(bcid);
    }

    public void enter() {
    }

    public void printMetadata() {
        getMetadata();
    }

    public void leave() {
        outputSB.append(json.toJSONString());
    }

    public boolean validIdentifier()  {
        if (this.bcid == null) {
            outputSB.append("{\"Identifier\":{\"status\":\"not found\"}}");
            return false;
        } else {
            return true;
        }
    }

    /**
     * get a JSONObject of the BcidMetadataSchema
     * @return
     */
    public JSONObject getMetadata() {
        json = new JSONObject();
        Field[] fields = getClass().getFields();
        for (Field field: fields) {
            // loop through all Fields of BcidMetadataSchema that are metadataElement
            if (field.getType().equals(metadataElement.class)) {
                try {
                    metadataElement element = (metadataElement) field.get(this);
                    JSONObject obj = new JSONObject();

                    obj.put("value", element.getValue());
                    obj.put("shortValue", element.getShortValue());
                    obj.put("fullKey", element.getFullKey());
                    obj.put("description", element.getDescription());

                    json.put(element.getKey(), obj);
                } catch (IllegalAccessException e) {
                    throw new ServerErrorException();
                }

            }
        }

        appendExpeditionOrDatasetData(resource);
        return json;
    }

    /**
     * check if the resource is a collection or dataset and append the dataset(s)
     * @param resource
     */
    private void appendExpeditionOrDatasetData(metadataElement resource) {
        ResourceTypes rts = new ResourceTypes();
        ResourceType rt = rts.get(resource.getValue());

        // check if the resource is a dataset or a collection
        if (rts.get(1).equals(rt)) {
            appendDataset();
        } else if (rts.get(38).equals(rt)) {
            appendExpeditionDatasets();
        }
    }

    private void appendExpeditionDatasets() {
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();
        if (displayDatasets()) {
            json.put("datasets", expeditionMinter.getDatasets(resolver.getExpeditionId()));
        }
    }

    private void appendDataset() {
        if (displayDatasets()) {
            JSONObject download = new JSONObject();
            String appRoot = sm.retrieveValue("appRoot");
            String projectId = resolver.getProjectID(resolver.getBcidId());

            // Excel option
            download.put("excel", appRoot + "query/excel?graphs=" + bcid.getGraph() + "&projectId=" + projectId);

            // TAB delimited option
            download.put("tab", appRoot + "query/tab?graphs=" + bcid.getGraph() + "&projectId=" + projectId);

            // n3 option
            download.put("n3", bcid.getWebAddress().toASCIIString());

            json.put("download", download);
        }
    }

    private Boolean displayDatasets() {
        Boolean ignoreUser = Boolean.getBoolean(sm.retrieveValue("ignoreUser"));
        Integer projectId = Integer.parseInt(resolver.getProjectID(resolver.getBcidId()));
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();
        ProjectMinter projectMinter = new ProjectMinter();

        try {
            //if public expedition, return true
            if (expeditionMinter.isPublic(resolver.getExpeditionCode(), projectId)) {
                return true;
            } else if (userId != null) {
                // if ignore_user and user in project, return true
                if (ignoreUser && projectMinter.userExistsInProject(userId, projectId)) {
                    return true;
                }
                // if !ignore_user and userOwnsExpedition, return true
                else if (!ignoreUser && expeditionMinter.userOwnsExpedition(userId, resolver.getExpeditionCode(), projectId)) {
                    return true;
                }
            }
        } finally {
            expeditionMinter.close();
            projectMinter.close();
        }

        return false;
    }

}
