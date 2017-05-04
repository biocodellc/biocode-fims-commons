package biocode.fims.projectConfig;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ProjectConfigValidatorTest {
    private final static String RESOURCE_URI = "http://www.w3.org/2000/01/rdf-schema#Resource";

    @Test
    public void invalid_if_no_mapping() {
        ProjectConfig config = new ProjectConfig(null, null, null);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
    }

    @Test
    public void invalid_if_entity_missing_concept_alias() {
        Mapping mapping = new Mapping();
        mapping.addEntity(entityNoConceptAlias());

        ProjectConfig config = new ProjectConfig(mapping, null, null);
        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity is missing a conceptAlias"), validator.errors());
    }

    @Test
    public void invalid_if_entity_non_unique_concept_alias() {
        Mapping mapping = new Mapping();
        mapping.addEntity(entity1());
        mapping.addEntity(entity1());

        ProjectConfig config = new ProjectConfig(mapping, null, null);
        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Duplicate entity conceptAlias detected \"resource1\""), validator.errors());
    }

    @Test
    public void invalid_if_entity_has_worksheet_no_unique_key() {
        Mapping mapping = new Mapping();
        Entity e = entity1();
        e.setUniqueKey("");
        mapping.addEntity(e);

        ProjectConfig config = new ProjectConfig(mapping, null, null);
        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies a worksheet but is missing a uniqueKey"), validator.errors());
    }

    @Test
    public void invalid_if_entity_parent_doesnt_exist() {
        Mapping mapping = new Mapping();
        Entity e1 = entity1();
        e1.setParentEntity("non_existant_concept_alias");
        mapping.addEntity(e1);

        ProjectConfig config = new ProjectConfig(mapping, null, null);
        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies a parent entity that does not exist"), validator.errors());
    }

    @Test
    public void invalid_if_entity_parent_no_unique_key() {
        Mapping mapping = new Mapping();

        Entity e1 = entity1();
        e1.setWorksheet("");
        e1.setUniqueKey("");
        mapping.addEntity(e1);

        Entity e2 = entity2();
        e2.setParentEntity(e1.getConceptAlias());
        mapping.addEntity(e2);

        ProjectConfig config = new ProjectConfig(mapping, null, null);
        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource2\" specifies a parent entity that is missing a uniqueKey"), validator.errors());
    }

    @Test
    public void invalid_if_attribute_with_DATETIME_dataType_missing_dataformat() {
        Mapping mapping = new Mapping();

        Attribute a = new Attribute("column5", "urn:column5");
        a.setDatatype(DataType.DATE);
        Entity e = entity1();
        e.addAttribute(a);
        mapping.addEntity(e);

        ProjectConfig config = new ProjectConfig(mapping, null, null);
        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies an attribute \"urn:column5\" with dataType \"DATE\" but is missing a dataFormat"), validator.errors());
    }

    private Entity entityNoConceptAlias() {
        Entity e = entity1();
        e.setConceptAlias("");
        return e;
    }

    private Entity entity1() {
        Entity e = new Entity();

        e.setConceptAlias("resource1");
        e.setConceptForwardingAddress("http://example.com");
        e.setConceptURI(RESOURCE_URI);
        e.setUniqueKey("column1");
        e.setWorksheet("worksheet1");

        attributesList1().forEach(e::addAttribute);

        return e;
    }

    private Entity entity2() {
        Entity e = new Entity();

        e.setConceptAlias("resource2");
        e.setConceptURI(RESOURCE_URI);
        e.setUniqueKey("column2");
        e.setWorksheet("worksheet2");

        attributesList1().forEach(e::addAttribute);

        return e;
    }

    private List<Attribute> attributesList1() {
        List<Attribute> attributes = new ArrayList<>();

        attributes.add(new Attribute("column1", "urn:column1"));
        attributes.add(new Attribute("column2", "urn:column2"));
        attributes.add(new Attribute("column3", "urn:column3"));
        attributes.add(new Attribute("column4", "urn:column4"));

        return attributes;
    }

}