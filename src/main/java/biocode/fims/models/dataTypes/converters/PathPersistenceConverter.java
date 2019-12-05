package biocode.fims.models.dataTypes.converters;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Convert Path Entity properties to String to be stored in the db
 */
@Converter
public class PathPersistenceConverter implements AttributeConverter<Path, String> {

    @Override
    public String convertToDatabaseColumn(Path entityValue) {
        return (entityValue == null) ? null : entityValue.toString();
    }

    @Override
    public Path convertToEntityAttribute(String databaseValue) {
        return (StringUtils.hasLength(databaseValue) ? Paths.get(databaseValue.trim()) : null);
    }
}
