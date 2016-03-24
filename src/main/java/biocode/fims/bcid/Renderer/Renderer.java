package biocode.fims.bcid.Renderer;

import biocode.fims.bcid.Bcid;
import biocode.fims.bcid.BcidMetadataSchema;

/**
 * Abstract class Renderer implements the visitor methods
 * and controls all renderer subClasses for rendering bcids
 */
public abstract class Renderer extends BcidMetadataSchema implements RendererInterface {
    protected StringBuilder outputSB;
    protected Bcid bcid;

    public Renderer(Bcid bcid) {
        this.bcid = bcid;
        BCIDMetadataInit(bcid);
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
