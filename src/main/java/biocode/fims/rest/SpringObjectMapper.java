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
        Hibernate5Module hm = new Hibernate5Module();
        hm.configure(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION, false);
        this.registerModule(hm);
        this.enable(SerializationFeature.INDENT_OUTPUT);
        this.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

}
