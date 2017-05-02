package biocode.fims.models.dataTypes.converters;

import biocode.fims.fimsExceptions.ServerErrorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.List;

/**
 * Convert List Entity properties to JSON String to be stored in the db
 */
@Converter
public class JSONArrayPersistenceConverter implements AttributeConverter<List, String> {
    private final static Logger logger = LoggerFactory.getLogger(JSONArrayPersistenceConverter.class);
    private final static ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new ServerErrorException("Server Error", "unable to convert list to json: " + ArrayUtils.toString(attribute));
        }
    }

    @Override
    public List convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, List.class);
        } catch (IOException e) {
            logger.error("IOException decoding json array from db: " + dbData);
            return null;
        }
    }
}
