package biocode.fims.entities;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.net.URI;

/**
 * Convert URI Entity properties to String to be stored in the db
 */
@Deprecated
@Converter
public class UriPersistenceConverter implements AttributeConverter<URI, String> {

    @Override
    public String convertToDatabaseColumn(URI entityValue) {
        return (entityValue == null) ? null : entityValue.toString();
    }

    @Override
    public URI convertToEntityAttribute(String databaseValue) {
        return (StringUtils.hasLength(databaseValue) ? URI.create(databaseValue.trim()) : null);
    }
}
