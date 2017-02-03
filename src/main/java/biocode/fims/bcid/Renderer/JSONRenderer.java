package biocode.fims.bcid.Renderer;

import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.bcid.*;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.bcid.BcidMetadataSchema.metadataElement;
import biocode.fims.service.BcidService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.json.simple.JSONObject;

import java.lang.reflect.Field;

/**
 * jsonRenderer renders objects as JSON
 */
public class JSONRenderer extends Renderer {
    private JSONObject json;
    private User user;

    private final ProjectAuthorizer projectAuthorizer;
    private final BcidService bcidService;
    private final String appRoot;

    /**
     * constructor for displaying private dataset information
     *
     */
    public JSONRenderer(User user, Bcid bcid, ProjectAuthorizer projectAuthorizer, BcidService bcidService,
                        BcidMetadataSchema bcidMetadataSchema, String appRoot) {
        super(bcid, bcidMetadataSchema);
        this.user = user;
        this.projectAuthorizer = projectAuthorizer;
        this.bcidService = bcidService;
        this.appRoot = appRoot;
    }

    public void enter() {
    }

    public void printMetadata() {
        getMetadata();
    }

    public void leave() {
        outputSB.append(json.toJSONString());
    }

    public boolean validIdentifier() {
        if (this.bcid == null) {
            outputSB.append("{\"Identifier\":{\"status\":\"not found\"}}");
            return false;
        } else {
            return true;
        }
    }

    /**
     * get a JSONObject of the BcidMetadataSchema
     *
     * @return
     */
    public JSONObject getMetadata() {
        json = new JSONObject();
        Field[] fields = bcidMetadataSchema.getClass().getFields();
        for (Field field : fields) {
            // loop through all Fields of BcidMetadataSchema that are metadataElement
            if (field.getType().equals(metadataElement.class)) {
                try {
                    metadataElement element = (metadataElement) field.get(bcidMetadataSchema);
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

        appendExpeditionOrDatasetData(bcidMetadataSchema.resource);
        return json;
    }

    /**
     * check if the resource is a collection or dataset and append the dataset(s)
     *
     * @param resource
     */
    private void appendExpeditionOrDatasetData(metadataElement resource) {
        // check if the resource is a dataset or a collection
        if (StringUtils.equals(ResourceTypes.DATASET_RESOURCE_TYPE, resource.getValue())) {
            appendDataset();
        } else if (StringUtils.equals(Expedition.EXPEDITION_RESOURCE_TYPE, resource.getValue())) {
            appendExpeditionDatasets();
        }
    }

    private void appendExpeditionDatasets() {
        Expedition expedition = bcid.getExpedition();
        if (expedition == null) {
            json.put("message", "This Expedition has been deleted. Contact the project administrator if you need access to the datasets");
        } else if (displayDatasets(expedition)) {
            JSONObject datasets = new JSONObject();
            datasets.put("datasets", bcidService.getDatasets(expedition.getProject().getProjectId(), expedition.getExpeditionCode()));
            datasets.put("appRoot", appRoot);
            json.put("datasets", datasets);
        }
    }

    private void appendDataset() {
        Expedition expedition = bcid.getExpedition();
        if (expedition == null) {
            json.put("message", "This dataset has been deleted. Contact the project administrator if you need access.");
        } else if (displayDatasets(expedition)) {

            // TODO add download link when we refactor BCID to separate application
        } else {
            json.put("message", "This is listed as a private dataset, You must be logged in to download data.");
        }
    }

    private Boolean displayDatasets(Expedition expedition) {
        return expedition != null && projectAuthorizer.userHasAccess(user, expedition.getProject());
    }

}
