package biocode.fims.bcid.Renderer;

import biocode.fims.entities.BcidTmp;
import biocode.fims.bcid.BcidMetadataSchema;

/**
 * Abstract class Renderer implements the visitor methods
 * and controls all renderer subClasses for rendering bcids
 */
public abstract class Renderer implements RendererInterface {
    protected StringBuilder outputSB;
    protected BcidTmp bcidTmp;
    protected BcidMetadataSchema bcidMetadataSchema;

    public Renderer(BcidTmp bcidTmp, BcidMetadataSchema bcidMetadataSchema) {
        this.bcidTmp = bcidTmp;
        this.bcidMetadataSchema = bcidMetadataSchema;
    }

    public void setBcidTmp(BcidTmp bcidTmp) {
        this.bcidTmp = bcidTmp;
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
