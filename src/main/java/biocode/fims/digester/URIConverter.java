package biocode.fims.digester;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.utils.EnumUtils;
import org.apache.commons.beanutils.Converter;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * {@link Converter} for apache commons BeanUtils to handle Enum type conversions
 */
class URIConverter implements Converter {

    @Override
    public Object convert(@SuppressWarnings("rawtypes") Class type, Object value) {
        try {
            return new URI((String) value);
        } catch (URISyntaxException e) {
            throw new FimsRuntimeException("invalid URI " + e.getMessage(), 500);
        }

    }
}
