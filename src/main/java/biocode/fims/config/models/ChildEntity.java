package biocode.fims.config.models;

import biocode.fims.config.Config;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;

import java.util.Collections;
import java.util.List;

/**
 * @author rjewing
 */
public abstract class ChildEntity extends DefaultEntity {


    ChildEntity() { // needed for EntityTypeIdResolver
        super();
    }

    public ChildEntity(String conceptAlias, String conceptUri) {
        super(conceptAlias, conceptUri);
    }

    @Override
    public void configure(Config config) {
        Entity parentEntity = config.entity(getParentEntity());

        if (parentEntity != null) {
            String uniqueKeyURI = parentEntity.getUniqueKeyURI();
            try {
                getAttributeByUri(uniqueKeyURI);
            } catch (FimsRuntimeException e) {
                if (e.getErrorCode() != ConfigCode.MISSING_ATTRIBUTE) {
                    throw e;
                }
                Attribute a = parentEntity.getAttributeByUri(uniqueKeyURI);
                addAttribute(a);
            }
        }
    }

    @Override
    public boolean isValid(Config config) {
        // NetworkConfigValidator does further validation of the parentEntity.
        // we just need to make sure it is set.
        return isChildEntity();
    }

    @Override
    public List<String> validationErrorMessages() {
        return Collections.singletonList("Entity \"" + getConceptAlias() + "\" is missing a valid parentEntity");

    }

    @Override
    public boolean canReload() {
        return false;
    }
}

