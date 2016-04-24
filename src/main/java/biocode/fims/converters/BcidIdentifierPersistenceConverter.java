package biocode.fims.converters;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Convert URI Entity properties to String to be stored in the db
 */
@Converter
public class BcidIdentifierPersistenceConverter implements AttributeConverter<URI, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(URI entityValue) {
        return (entityValue == null) ? null : entityValue.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public URI convertToEntityAttribute(byte[] databaseValue) {
        return (StringUtils.hasLength(String.valueOf(databaseValue)) ? URI.create(String.valueOf(databaseValue).trim()) : null);
    }
}
