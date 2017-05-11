package biocode.fims.projectConfig;

import biocode.fims.digester.*;
import biocode.fims.validation.rules.CompositeUniqueValueRule;
import biocode.fims.validation.rules.ControlledVocabularyRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ProjectConfigValidatorTest {
    private final static String RESOURCE_URI = "http://www.w3.org/2000/01/rdf-schema#Resource";

    @Test(expected = IllegalArgumentException.class)
    public void invalid_if_no_config() {

        new ProjectConfigValidator(null);
    }

    @Test
    public void invalid_if_entity_missing_concept_alias() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(new Entity(""));

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity is missing a conceptAlias"), validator.errors());
    }

    @Test
    public void invalid_if_entity_non_unique_concept_alias() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(entity1());
        config.addEntity(new Entity("resource1"));

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Duplicate entity conceptAlias detected \"resource1\""), validator.errors());
    }

    @Test
    public void invalid_if_entity_unique_key_missing_attribute() {
        ProjectConfig config = new ProjectConfig();
        Entity entity = entity1();
        entity.setUniqueKey("column_non_exist");
        config.addEntity(entity);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies a uniqueKey but can not find an Attribute with a matching column"), validator.errors());
    }

    @Test
    public void invalid_if_entity_has_worksheet_no_unique_key() {
        ProjectConfig config = new ProjectConfig();
        Entity e = entity1();
        e.setUniqueKey("");
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies a worksheet but is missing a uniqueKey"), validator.errors());
    }

    @Test
    public void invalid_if_entity_parent_doesnt_exist() {
        ProjectConfig config = new ProjectConfig();
        Entity e1 = entity1();
        e1.setParentEntity("non_existant_concept_alias");
        config.addEntity(e1);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies a parent entity that does not exist"), validator.errors());
    }

    @Test
    public void invalid_if_entity_parent_no_unique_key() {
        ProjectConfig config = new ProjectConfig();

        Entity e1 = entity1();
        e1.setWorksheet("");
        e1.setUniqueKey("");
        config.addEntity(e1);

        Entity e2 = entity2();
        e2.setParentEntity(e1.getConceptAlias());
        config.addEntity(e2);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource2\" specifies a parent entity that is missing a uniqueKey"), validator.errors());
    }

    @Test
    public void invalid_if_no_attribute_for_entity_parent_unique_key() {
        ProjectConfig config = new ProjectConfig();

        Entity e1 = entity1();
        e1.setUniqueKey("uniqueColumn");
        e1.addAttribute(new Attribute("uniqueColumn", "urn:uniqueColumn"));
        config.addEntity(e1);

        Entity e2 = entity2();
        e2.setParentEntity(e1.getConceptAlias());
        config.addEntity(e2);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource2\" specifies a parent entity but is missing an attribute for the parent entity uniqueKey"), validator.errors());
    }

    @Test
    public void invalid_if_attribute_with_DATETIME_dataType_missing_dataformat() {
        ProjectConfig config = new ProjectConfig();

        Attribute a = new Attribute("column5", "urn:column5");
        a.setDatatype(DataType.DATE);
        Entity e = entity1();
        e.addAttribute(a);
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies an attribute \"urn:column5\" with dataType \"DATE\" but is missing a dataFormat"), validator.errors());
    }

    @Test
    public void invalid_if_rule_configuration_invalid() {
        ProjectConfig config = new ProjectConfig();

        Entity e = entity1();
        e.addRule(new ControlledVocabularyRule("column1", "noList"));
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Invalid Project configuration. Could not find list with name \"noList\""), validator.errors());
    }

    @Test
    public void invalid_if_entity_attributes_have_duplicate_uri() {
        ProjectConfig config = new ProjectConfig();

        Entity e = entity1();
        e.addAttribute(new Attribute("duplicateUri", "urn:column1"));
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Duplicate Attribute uri \"urn:column1\" found in entity \"resource1\""), validator.errors());
    }

    private Entity entity1() {
        Entity e = new Entity("resource1");

        e.setConceptForwardingAddress("http://example.com");
        e.setConceptURI(RESOURCE_URI);
        e.setUniqueKey("column1");
        e.setWorksheet("worksheet1");

        attributesList1().forEach(e::addAttribute);

        return e;
    }

    private Entity entity2() {
        Entity e = new Entity("resource2");

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