package biocode.fims.bcid.Renderer;

import biocode.fims.bcid.Bcid;
import biocode.fims.bcid.BcidMetadataSchema;

/**
 * Abstract class Renderer implements the visitor methods
 * and controls all renderer subClasses for rendering bcids
 */
public abstract class Renderer extends BcidMetadataSchema implements RendererInterface {
    protected StringBuilder outputSB;

    /**
     * render an Identifier
     *
     * @return
     */
    public String render(Bcid bcid) {
        BCIDMetadataInit(bcid);
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
