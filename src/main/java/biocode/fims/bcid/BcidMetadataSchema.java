package biocode.fims.bcid;

import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.settings.SettingsManager;
import org.springframework.util.StringUtils;

/**
 * Metadata Schema for Describing a Bcid--
 * These are the metadata elements building blocks that we can use to express this Bcid either via RDF or HTML
 * and consequently forms the basis of what the "outside" world sees about the bcids.
 * This class is used by Renderers to structure content.
 */
public class BcidMetadataSchema {
    // Core Elements for rendering
    public metadataElement about;
    public metadataElement resource;
    public metadataElement dcCreator;
    public metadataElement dcTitle;
    public metadataElement dcDate;
    public metadataElement dcRights;
    public metadataElement dcIsReferencedBy;
    public metadataElement dcIsPartOf;
    public metadataElement dcSource;
    public metadataElement dcMediator;
    public metadataElement dcHasVersion;
    public metadataElement dcPublisher;
    public metadataElement forwardingResolution;
    public metadataElement resolutionTarget;
    public metadataElement identifier;
    public metadataElement isPublic;

    private final Bcid bcid;
    private final Identifier identifierObject;
    private final SettingsManager settingsManager;

    public BcidMetadataSchema(Bcid bcid, SettingsManager settingsManager, Identifier identifier) {
        this.bcid = bcid;
        this.identifierObject = identifier;
        this.settingsManager = settingsManager;
        registerMetadataElements();
        setMetadataElements();
    }

    private void setMetadataElements() {
        Expedition expedition = bcid.getExpedition();
        if (expedition != null && expedition.getProject() != null) {
            dcPublisher.setValue(expedition.getProject().getProjectCode());
            isPublic.setValue(String.valueOf(expedition.getProject().isPublic()));
        }

        identifier.setValue(String.valueOf(bcid.getIdentifier()));
        about.setValue(settingsManager.retrieveValue("resolverTargetPrefix") + identifierObject.getIdentifier());
        resource.setValue(bcid.getResourceType());

        dcDate.setValue(String.valueOf(bcid.getTs()));
        dcCreator.setValue(bcid.getUser().getFullName());
        dcTitle.setValue(bcid.getTitle());
        dcSource.setValue(identifierObject.getSuffix());
        dcRights.setValue(settingsManager.retrieveValue("rights"));
        dcHasVersion.setValue(String.valueOf(bcid.getWebAddress()));
        dcMediator.setValue(settingsManager.retrieveValue("resolverMetadataPrefix"));

        if (!bcid.getUser().getFullName().equals("Test Account")) {
            dcIsReferencedBy.setValue("http://n2t.net/" + bcid.getIdentifier());
        }

        if (bcid.getDoi() != null) {
            String doi = bcid.getDoi().replace("doi:", "http://dx.doi.org/");
            dcIsPartOf.setValue(doi);
        }


        if (hasForwardingResolution()) {
            forwardingResolution.setValue(String.valueOf(true));
            resolutionTarget.setValue(bcid.getWebAddress() + identifierObject.getSuffix());
        }

    }

    private boolean hasForwardingResolution() {
        if (!StringUtils.isEmpty(bcid.getWebAddress()) &&
                !StringUtils.isEmpty(identifierObject.getSuffix())) {
            return true;
        }
        return false;
    }

    /**
     * Register all of the bcid metadataElements
     */
    private void registerMetadataElements() {
        dcPublisher = new metadataElement("dc:publisher", "Unassigned to a project", "The BCID project to which this resource belongs.");
        about = new metadataElement("rdf:Description", "", "The current bcid resolution service.");
        resource = new metadataElement("rdf:type", "", "What is this object.");
        dcDate = new metadataElement("dc:date",  "", "Date that metadata was last updated for this bcid.");
        dcCreator = new metadataElement("dc:creator", "", "Who created the group definition.");
        dcTitle = new metadataElement("dc:title", "", "Title");
        dcSource = new metadataElement("dc:source", "", "The locally-unique bcid.");
        dcRights = new metadataElement("dcterms:rights", "", "Rights applied to the metadata content describing this bcid.");
        dcIsReferencedBy = new metadataElement("dcterms:isReferencedBy", "", "The group level bcid, registered with EZID.");
        dcIsPartOf = new metadataElement("dcterms:isPartOf", "", "A DOI describing the dataset which this bcid belongs to.");
        dcHasVersion = new metadataElement("dcterms:hasVersion", "", "The redirection target for this bcid.");
        forwardingResolution = new metadataElement("urn:forwardingResolution", "false", "Indicates that this bcid has a suffix and should be forwarded to the fowardingResolutionTarget.");
        resolutionTarget = new metadataElement("urn:resolutionTarget", "", "The target uri for the locally-unique bcid.");
        identifier = new metadataElement("identifier", "", "The identifier this metadata represents.");
        dcMediator = new metadataElement("dcterms:mediator", "", "Metadata mediator");
        isPublic = new metadataElement("urn:isPublic", "false", "If this bcid is publicly viewable");
    }

    /**
     * A convenience class for holding metadata elements
     */
    public final class metadataElement {
        private String key;
        private String value;
        private String description;

        metadataElement(String key, String value, String description) {
            this.key = key;
            this.value = value;
            this.description = description;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public void setValue(String value) {
            if (value == null) {
                this.value = "";
            } else {
                this.value = value;
            }
        }

        /**
         * Replace prefixes with fully qualified URL's
         *
         * @return
         */
        public String getFullKey() {
            String tempKey = key;
            tempKey = tempKey.replace("dc:", "http://purl.org/dc/elements/1.1/");
            tempKey = tempKey.replace("dcterms:", "http://purl.org/dc/terms/");
            tempKey = tempKey.replace("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
            tempKey = tempKey.replace("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            tempKey = tempKey.replace("bsc:", "http://biscicol.org/terms/index.html#");
            return tempKey;
        }

        /**
         * Return the human readable name of the uri if possible
         * @return
         */
        public String getShortValue() {
            String[] splitValue = value.split("/");
            if (splitValue.length > 0) {
                return splitValue[splitValue.length - 1];
            }
            return value;
        }
    }
}
