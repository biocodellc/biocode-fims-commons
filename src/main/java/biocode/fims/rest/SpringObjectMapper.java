package biocode.fims.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

/**
 * Jackson ObjectMapper with hibernate support
 */
@Component
public class SpringObjectMapper extends ObjectMapper {

    public SpringObjectMapper() {
        this.registerModule(new Hibernate5Module());
        this.enable(SerializationFeature.INDENT_OUTPUT);
        this.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

}
