package biocode.fims.config.models;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.EnumSet;
import java.util.Set;

import static biocode.fims.fimsExceptions.errorCodes.ConfigCode.MISSING_ATTRIBUTE;

/**
 * This is a child entity that contains hardcoded properties.
 *
 * These properties will be added to the entity and ensure that they are not removed
 *
 * @author rjewing
 */
public abstract class PropEntity<E extends Enum<E> & EntityProps> extends ChildEntity {
    private Set<E> values;

    protected PropEntity(Class<E> c) {
        super();
        this.values = EnumSet.allOf(c);
    }

    protected PropEntity(Class<E> c, String conceptAlias, String conceptUri) {
        super(conceptAlias, conceptUri);
        this.values = EnumSet.allOf(c);
        init();
    }

    protected void init() {
        for (EntityProps p : values) {
            try {
                Attribute a = getAttribute(p.uri());
                a.setUri(p.uri());
                a.setDataType(DataType.STRING);
            } catch (FimsRuntimeException e) {
                if (e.getErrorCode() == MISSING_ATTRIBUTE) {
                    addAttribute(new Attribute(p.column(), p.uri()));
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * class used to verify PhotoEntity data integrity after deserialization. This is necessary
     * so we don't overwrite the default values during deserialization.
     */
    protected static abstract class PropEntitySanitizer<T extends PropEntity> extends StdConverter<T, T> {
        PropEntitySanitizer() {
            super();
        }

        @Override
        public T convert(T value) {
            value.init();
            return value;
        }
    }
}

