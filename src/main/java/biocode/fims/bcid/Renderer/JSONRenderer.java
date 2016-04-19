package biocode.fims.bcid.Renderer;

import biocode.fims.bcid.*;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.repository.ExpeditionRepository;
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

    private SettingsManager settingsManager;
    private ExpeditionRepository expeditionRepository;

    /**
     * constructor for displaying private dataset information
     * @param username
     */
    public JSONRenderer(String username, Bcid bcid, ExpeditionRepository expeditionRepository, SettingsManager settingsManager) {
        super(bcid);
        userId = BcidDatabase.getUserId(username);
        this.expeditionRepository = expeditionRepository;
        this.settingsManager = settingsManager;
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
        Expedition expedition = expeditionRepository.findByBcid(bcid);
        if (displayDatasets(expedition)) {
            JSONObject datasets = new JSONObject();
            datasets.put("datasets", expeditionMinter.getDatasets(expedition.getExpeditionId()));
            datasets.put("appRoot", settingsManager.retrieveValue("appRoot"));
            json.put("datasets", datasets);
        }
    }

    private void appendDataset() {
        Expedition expedition = expeditionRepository.findByBcid(bcid);
        if (displayDatasets(expedition) && bcid.getGraph() != null) {
            JSONObject download = new JSONObject();
            String appRoot = settingsManager.retrieveValue("appRoot");

            // Excel option
            download.put("graph", bcid.getGraph());
            download.put("projectId", expedition.getProjectId());
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

    private Boolean displayDatasets(Expedition expedition) {
        Boolean ignoreUser = Boolean.getBoolean(settingsManager.retrieveValue("ignoreUser"));
        ProjectMinter projectMinter = new ProjectMinter();

        if (expedition != null) {

            //if public expedition, return true
            if (expedition.isPublic()) {
                return true;
            } else if (userId != null) {

                // if ignore_user and user in project, return true
                if (ignoreUser && projectMinter.userExistsInProject(userId, expedition.getProjectId())) {
                    return true;
                }
                // if !ignore_user and userOwnsExpedition, return true
                else if (!ignoreUser && expedition.getUserId() == userId) {
                    return true;
                }
            }

        }
        return false;
    }

}
