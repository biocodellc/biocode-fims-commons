package biocode.fims.projectConfig;

import biocode.fims.models.ExpeditionMetadataProperty;
import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.DataType;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.validation.rules.ControlledVocabularyRule;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.*;
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
    public void invalid_if_entity_missing_conceptURI() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(new Entity("testing", ""));

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"testing\" is missing a conceptURI"), validator.errors());
    }

    @Test
    public void invalid_if_entity_missing_concept_alias() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(new Entity("", "someURI"));

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity is missing a conceptAlias"), validator.errors());
    }

    @Test
    public void invalid_if_entity_non_unique_concept_alias() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(entity1());
        config.addEntity(new Entity("Resource1", "someURI"));

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Duplicate entity conceptAlias detected \"Resource1\". conceptAliases are not case sensitive."), validator.errors());
    }

    @Test
    public void invalid_if_entity_concept_alias_not_sql_safe() {
        ProjectConfig config = new ProjectConfig();
        config.addEntity(new Entity("resource1;select *", "someURI"));

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity conceptAlias contains one or more invalid characters. Only letters, digits, and _ are valid"), validator.errors());
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
        assertEquals(Arrays.asList("Entity \"resource2\" specifies a parent entity but is missing an attribute for the parent entity uniqueKey: \"uniqueColumn\""), validator.errors());
    }

    @Test
    public void invalid_if_attribute_with_DATETIME_dataType_missing_dataformat() {
        ProjectConfig config = new ProjectConfig();

        Attribute a = new Attribute("column5", "urn:column5");
        a.setDataType(DataType.DATE);
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
        e.addRule(new ControlledVocabularyRule("column1", "noList", config));
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

    @Test
    public void invalid_if_entity_attributes_have_duplicate_column() {
        ProjectConfig config = new ProjectConfig();

        Entity e = entity1();
        e.addAttribute(new Attribute("column1", "urn:/column_1"));
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Duplicate Attribute column \"column1\" found in entity \"resource1\""), validator.errors());
    }

    @Test
    public void invalid_if_entity_attribute_uri_invalid() {
        ProjectConfig config = new ProjectConfig();

        Entity e = entity1();
        e.addAttribute(new Attribute("column6", null));
        e.addAttribute(new Attribute("column7", "some uri"));
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        List<String> expected = Arrays.asList(
                "Invalid Attribute uri \"null\" found in entity \"resource1\". Uri must only contain alpha-numeric or _:/ characters.",
                "Invalid Attribute uri \"some uri\" found in entity \"resource1\". Uri must only contain alpha-numeric or _:/ characters."
        );

        assertFalse(validator.isValid());
        assertEquals(expected, validator.errors());

    }

    @Test
    public void invalid_if_expeditionMetadataProperty_missing_name() {
        ProjectConfig config = new ProjectConfig();

        List<ExpeditionMetadataProperty> m = new ArrayList<>();
        m.add(new ExpeditionMetadataProperty(" ", false));
        config.setExpeditionMetadataProperties(m);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        List<String> expected = Arrays.asList("ExpeditionMetadataProperty is missing a name.");

        assertFalse(validator.isValid());
        assertEquals(expected, validator.errors());

    }

    @Test
    public void invalid_if_entity_isValid_returns_false() {
        ProjectConfig config = new ProjectConfig();
        Entity e = customEntity();
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Test entity validation message"), validator.errors());
    }

    private Entity entity1() {
        Entity e = new Entity("resource1", "someURI");

        e.setConceptForwardingAddress("http://example.com");
        e.setConceptURI(RESOURCE_URI);
        e.setUniqueKey("column1");
        e.setWorksheet("worksheet1");

        attributesList1().forEach(e::addAttribute);

        return e;
    }

    private Entity entity2() {
        Entity e = new Entity("resource2", "someURI");

        e.setConceptURI(RESOURCE_URI);
        e.setUniqueKey("column2");
        e.setWorksheet("worksheet2");

        attributesList1().forEach(e::addAttribute);

        return e;
    }

    private Entity customEntity() {
        Entity e = new Entity("resource1", "someURI"){
            @Override
            public boolean isValid(ProjectConfig config) {
                Assert.notNull(config, "ProjectConfig is null");
                return false;
            }

            @Override
            public List<String> validationErrorMessages() {
                return Collections.singletonList("Test entity validation message");
            }
        };

        e.setConceptForwardingAddress("http://example.com");
        e.setConceptURI(RESOURCE_URI);
        e.setUniqueKey("column1");
        e.setWorksheet("worksheet1");

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