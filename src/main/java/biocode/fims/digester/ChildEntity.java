package biocode.fims.digester;

/**
 * @author rjewing
 */
public class ChildEntity extends AbstractEntity {

    private String parentEntityConceptAlias;

    public String getParentEntityConceptAlias() {
        return parentEntityConceptAlias;
    }

    public void setParentEntityConceptAlias(String parentEntityConceptAlias) {
        this.parentEntityConceptAlias = parentEntityConceptAlias;
    }
}
