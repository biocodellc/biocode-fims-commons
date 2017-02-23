package biocode.fims.digester;

/**
 * @author rjewing
 */
public class ChildEntity extends AbstractEntity {

    private String parentEntity;

    public String getParentEntity() {
        return parentEntity;
    }

    public void setParentEntity(String parentEntity) {
        this.parentEntity = parentEntity;
    }
}
