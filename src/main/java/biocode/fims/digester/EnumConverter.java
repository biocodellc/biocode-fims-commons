package biocode.fims.digester;

import biocode.fims.utils.EnumUtils;
import org.apache.commons.beanutils.Converter;

/**
 * {@link Converter} for apache commons BeanUtils to handle Enum type conversions
 */
class EnumConverter implements Converter {

    @Override
    public Object convert(@SuppressWarnings("rawtypes") Class type, Object value) {
        if (type.isEnum()) {
            return EnumUtils.lookup(type, (String) value);
        }
        return null;
    }
}
