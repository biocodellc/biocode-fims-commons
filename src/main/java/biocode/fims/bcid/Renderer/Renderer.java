package biocode.fims.bcid.Renderer;

import biocode.fims.entities.Bcid;
import biocode.fims.bcid.BcidMetadataSchema;

/**
 * Abstract class Renderer implements the visitor methods
 * and controls all renderer subClasses for rendering bcids
 */
public abstract class Renderer implements RendererInterface {
    protected StringBuilder outputSB;
    protected Bcid bcid;
    protected BcidMetadataSchema bcidMetadataSchema;

    public Renderer(Bcid bcid, BcidMetadataSchema bcidMetadataSchema) {
        this.bcid = bcid;
        this.bcidMetadataSchema = bcidMetadataSchema;
    }

    public void setBcid(Bcid bcid) {
        this.bcid = bcid;
    }

    /**
     * render an Identifier
     *
     * @return
     */
    public String render() {
        outputSB = new StringBuilder();

        if (validIdentifier()) {
            enter();
            printMetadata();
            leave();
            return outputSB.toString();
        } else {
            return outputSB.toString();
        }
    }
}
