package biocode.fims.bcid.Renderer;

import biocode.fims.bcid.BcidMetadataSchema;
import biocode.fims.entities.Bcid;

/**
 * Renders a BCID as RDF.  This is for machine negotiation of an Bcid
 */
public class RDFRenderer extends Renderer {

    public RDFRenderer(Bcid bcid, BcidMetadataSchema bcidMetadataSchema) {
        super(bcid, bcidMetadataSchema);
    }

    public void enter() {
        outputSB.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "\txmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                "\txmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
                "\txmlns:bsc=\"http://biscicol.org/terms/index.html#\"\n" +
                "\txmlns:dcterms=\"http://purl.org/dc/terms/\">\n");
        outputSB.append("<rdf:Description rdf:about=\"" + this.bcid.toString() + "\">\n");
    }

    public void printMetadata() {
        resourceAppender(bcidMetadataSchema.resource);
        resourceAppender(bcidMetadataSchema.dcIsPartOf);
        resourceAppender(bcidMetadataSchema.dcRights);
        resourceAppender(bcidMetadataSchema.dcIsReferencedBy);
        propertyAppender(bcidMetadataSchema.dcTitle);
        propertyAppender(bcidMetadataSchema.dcPublisher);
        propertyAppender(bcidMetadataSchema.dcCreator);
        propertyAppender(bcidMetadataSchema.dcDate);
        propertyAppender(bcidMetadataSchema.dcSource);
    }

    public void leave() {
        outputSB.append("</rdf:Description>\n");
        outputSB.append("</rdf:RDF>");
    }

    public boolean validIdentifier() {
        if (this.bcid == null) {
            outputSB.append("bcid is null");
            return false;
        } else {
            return true;
        }
    }

    /**
     * append the rdf:Resource
     *
     * @param map
     */
    private void resourceAppender(BcidMetadataSchema.metadataElement map) {
        if (map != null) {
            if (!map.getValue().trim().equals("")) {
                outputSB.append("\t<" + map.getKey() + " rdf:resource=\"" + map.getValue() + "\" />\n");
            }
        }
    }

    /**
     * append each property
     *
     * @param map
     */
    private void propertyAppender(BcidMetadataSchema.metadataElement map) {
        //TODO should we silence this exception?
        try {
        if (map != null) {
            if (!map.getValue().trim().equals("")) {
                outputSB.append("\t<" + map.getKey() + ">" + map.getValue() + "</" + map.getKey() + ">\n");
            }
        }
        } catch (Exception e) {
            // fail silently
        }

    }
}
