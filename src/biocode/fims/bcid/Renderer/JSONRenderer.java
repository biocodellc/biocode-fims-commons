package biocode.fims.bcid.Renderer;

import biocode.fims.bcid.*;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.SettingsManager;
import org.apache.commons.validator.routines.UrlValidator;
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
                    UrlValidator urlValidator = new UrlValidator();
                    obj.put("isResource", (urlValidator.isValid(element.getValue())));

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
            JSONObject datasets = new JSONObject();
            datasets.put("datasets", expeditionMinter.getDatasets(resolver.getExpeditionId()));
            datasets.put("appRoot", sm.retrieveValue("appRoot"));
            json.put("datasets", datasets);
        }
    }

    private void appendDataset() {
        if (displayDatasets() && bcid.getGraph() != null) {
            JSONObject download = new JSONObject();
            String appRoot = sm.retrieveValue("appRoot");

            String projectId = resolver.getProjectID(resolver.getBcidId());

            // Excel option
            download.put("graph", bcid.getGraph());
            download.put("projectId", projectId);
            download.put("appRoot", appRoot);

            // n3 option
            download.put("n3", (bcid.getWebAddress() != null) ? bcid.getWebAddress().toASCIIString() : null);

            json.put("download", download);
        } else if (bcid.getGraph() != null) {
            JSONObject msg = new JSONObject();
            msg.put("message", "This is listed as a private dataset, You must be logged in to download data.");

            json.put("download", msg);
        }
    }

    private Boolean displayDatasets() {
        Boolean ignoreUser = Boolean.getBoolean(sm.retrieveValue("ignoreUser"));
        Integer projectId;
        try {
            projectId = Integer.parseInt(resolver.getProjectID(resolver.getBcidId()));
        } catch (Exception e) {
            // TODO: come up with a cleaner way to detect unassociated data.
            // if there is an exception here, then return data... unassociated project
            // data is public
            return true;
        }
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
