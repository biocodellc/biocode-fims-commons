package biocode.fims.serializers;

import biocode.fims.digester.Entity;
import biocode.fims.digester.TestEntity;
import biocode.fims.rest.FimsObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.fail;

/**
 * @author rjewing
 */
public class EntityTypeIdResolverTest {

    private static ObjectMapper mapper;

    @BeforeClass
    public static void setUp() {
        mapper = new FimsObjectMapper();
    }

    @Test
    public void should_serialize_and_deserialize_entity_implementation() throws IOException {
        TestEntity entity = new TestEntity();

        String serialized = mapper.writeValueAsString(entity);
        Entity deserialized = mapper.readValue(serialized, Entity.class);

        assertTrue(deserialized instanceof TestEntity);
        assertEquals(TestEntity.UNIQUE_KEY, deserialized.getUniqueKey());
        assertEquals(TestEntity.TYPE, deserialized.type());
    }

    @Test
    public void should_throw_exception_if_entity_implementation_not_found() throws IOException {
        String entityString = "{\"type\": \"non existent entity\"}";
        try {
            Entity entity = mapper.readValue(entityString, Entity.class);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Could not find Entity subclass with type: \"non existent entity\" in package: biocode.fims.digester", e.getMessage());
        }
    }

    @Test(expected = JsonMappingException.class)
    public void should_throw_exception_if_entity_name_missing_from_json() throws IOException {
        String entityString = "{\"property1\": \"some property\"}";
        Entity entity = mapper.readValue(entityString, Entity.class);
    }

    @Test
    public void should_throw_exception_if_entity_implementation_in_wrong_package() throws IOException {
        Entity entity = new EntityInWrongPackage();
        try {
            mapper.writeValueAsString(entity);
            fail();
        } catch (JsonMappingException e) {
            assertEquals("class biocode.fims.serializers.EntityInWrongPackage is not in the package biocode.fims.digester", e.getMessage());
        }
    }

    @Test
    public void should_throw_exception_if_EntityTypeIdResolver_used_on_non_Entity_implementation() throws IOException {
        NonEntityTypeIdResolverTestClass entity = new NonEntityTypeIdResolverTestClass();
        try {
            mapper.writeValueAsString(entity);
            fail();
        } catch (JsonMappingException e) {
            assertEquals("class biocode.fims.serializers.NonEntityTypeIdResolverTestClass is not a subclass of the Entity class", e.getMessage());
        }
    }

}