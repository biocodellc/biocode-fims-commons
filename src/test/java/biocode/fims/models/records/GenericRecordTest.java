package biocode.fims.models.records;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class GenericRecordTest {

    @Test
    public void should_return_empty_string_if_property_doesnt_exist() {
        GenericRecord record = new GenericRecord();

        assertEquals("", record.get("non_existant_property"));
    }

    @Test
    public void should_return_property_if_property_exists() {
        GenericRecord record = new GenericRecord();
        record.set("property1", "test_value");

        assertEquals("test_value", record.get("property1"));
    }

    @Test
    public void should_load_properties_in_constructor() {
        Map<String, String> properties = new HashMap<>();
        properties.put("property1", "test_value");
        properties.put("property2", "test_value2");

        GenericRecord record = new GenericRecord(properties);

        assertEquals("test_value", record.get("property1"));
        assertEquals("test_value2", record.get("property2"));
    }



}