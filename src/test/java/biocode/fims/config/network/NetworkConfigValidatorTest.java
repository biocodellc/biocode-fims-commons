package biocode.fims.config.network;

import biocode.fims.config.Config;
import biocode.fims.config.models.DefaultEntity;
import biocode.fims.models.ExpeditionMetadataProperty;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DataType;
import biocode.fims.config.models.Entity;
import biocode.fims.validation.rules.ControlledVocabularyRule;
import biocode.fims.validation.rules.RequiredValueRule;
import biocode.fims.validation.rules.RuleLevel;
import biocode.fims.validation.rules.UniqueValueRule;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.*;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class NetworkConfigValidatorTest {
    private final static String RESOURCE_URI = "http://www.w3.org/2000/01/rdf-schema#Resource";

    @Test(expected = IllegalArgumentException.class)
    public void invalid_if_no_config() {
        new NetworkConfigValidator(null);
    }

    @Test
    public void invalid_if_entity_missing_conceptURI() {
        NetworkConfig config = new NetworkConfig();
        config.addEntity(new DefaultEntity("testing", ""));

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"testing\" is missing a conceptURI"), validator.errors());
    }

    @Test
    public void invalid_if_entity_missing_concept_alias() {
        NetworkConfig config = new NetworkConfig();
        config.addEntity(new DefaultEntity("", "someURI"));

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity is missing a conceptAlias"), validator.errors());
    }

    @Test
    public void invalid_if_entity_non_unique_concept_alias() {
        NetworkConfig config = new NetworkConfig();
        config.addEntity(entity1());
        config.addEntity(new DefaultEntity("Resource1", "someURI"));

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Duplicate entity conceptAlias detected \"Resource1\". conceptAliases are not case sensitive."), validator.errors());
    }

    @Test
    public void invalid_if_entity_concept_alias_not_sql_safe() {
        NetworkConfig config = new NetworkConfig();
        config.addEntity(new DefaultEntity("resource1;select *", "someURI"));

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity conceptAlias contains one or more invalid characters. Only letters, digits, and _ are valid"), validator.errors());
    }

    @Test
    public void invalid_if_entity_unique_key_missing_attribute() {
        NetworkConfig config = new NetworkConfig();
        Entity entity = entity1();
        entity.setUniqueKey("column_non_exist");
        config.addEntity(entity);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies a uniqueKey but can not find an Attribute with a matching column"), validator.errors());
    }

    @Test
    public void invalid_if_entity_has_worksheet_no_unique_key() {
        NetworkConfig config = new NetworkConfig();
        Entity e = entity1();
        e.setUniqueKey("");
        config.addEntity(e);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies a worksheet but is missing a uniqueKey"), validator.errors());
    }

    @Test
    public void invalid_if_entity_parent_doesnt_exist() {
        NetworkConfig config = new NetworkConfig();
        Entity e1 = entity1();
        e1.setParentEntity("non_existant_concept_alias");
        config.addEntity(e1);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies a parent entity that does not exist"), validator.errors());
    }

    @Test
    public void invalid_if_entity_parent_no_unique_key() {
        NetworkConfig config = new NetworkConfig();

        Entity e1 = entity1();
        e1.setWorksheet("");
        e1.setUniqueKey("");
        config.addEntity(e1);

        Entity e2 = entity2();
        e2.setParentEntity(e1.getConceptAlias());
        config.addEntity(e2);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource2\" specifies a parent entity that is missing a uniqueKey"), validator.errors());
    }

    @Test
    public void invalid_if_no_attribute_for_entity_parent_unique_key() {
        NetworkConfig config = new NetworkConfig();

        Entity e1 = entity1();
        e1.setUniqueKey("uniqueColumn");
        e1.addAttribute(new Attribute("uniqueColumn", "urn:uniqueColumn"));
        config.addEntity(e1);

        Entity e2 = entity2();
        e2.setParentEntity(e1.getConceptAlias());
        config.addEntity(e2);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource2\" specifies a parent entity but is missing an attribute for the parent entity uniqueKey: \"uniqueColumn\""), validator.errors());
    }

    @Test
    public void invalid_if_entity_unique_across_project_but_not_parent_unique_across_project() {
        NetworkConfig config = new NetworkConfig();

        Entity e1 = entity1();
        config.addEntity(e1);

        Entity e2 = entity2();
        e2.setParentEntity(e1.getConceptAlias());
        e2.setUniqueAcrossProject(true);
        config.addEntity(e2);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource2\" requires the key to be unique across the entire project, but the parentEntity is not unique across the project."), validator.errors());
    }

    @Test
    public void invalid_if_parent_unique_key_uri_not_in_child_attributes() {
        NetworkConfig config = new NetworkConfig();

        Entity e1 = entity1();
        config.addEntity(e1);

        Entity e2 = entity2();
        e2.getAttribute(e1.getUniqueKey()).setUri("something_different");
        e2.setParentEntity(e1.getConceptAlias());
        config.addEntity(e2);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource2\" specifies a parent entity but the attribute for the parent entity uniqueKey: \"column1\" has a different uri: \"something_different\" instead of \"urn:column1\""), validator.errors());
    }

    @Test
    public void invalid_if_attribute_with_DATETIME_dataType_missing_dataformat() {
        NetworkConfig config = new NetworkConfig();

        Attribute a = new Attribute("column5", "urn:column5");
        a.setDataType(DataType.DATE);
        Entity e = entity1();
        e.addAttribute(a);
        config.addEntity(e);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"resource1\" specifies an attribute \"urn:column5\" with dataType \"DATE\" but is missing a dataFormat"), validator.errors());
    }

    @Test
    public void invalid_if_rule_configuration_invalid() {
        NetworkConfig config = new NetworkConfig();

        Entity e = entity1();
        e.addRule(new ControlledVocabularyRule("column1", "noList", config));
        config.addEntity(e);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Invalid Project configuration. Could not find list with name \"noList\""), validator.errors());
    }

    @Test
    public void invalid_if_unique_value_rule_uniqueAcrossProject_for_uniqueKey_but_not_entity_uniqueAcrossProject() {
        NetworkConfig config = new NetworkConfig();

        Entity e = entity1();
        e.addRule(new UniqueValueRule("column1", true, RuleLevel.ERROR));
        config.addEntity(e);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("UniqueValueRule for uniqueKey column: \"column1\" has uniqueAcrossProject = true, however entity: \"resource1\" uniqueAcrossProject = false"), validator.errors());
    }

    @Test
    public void invalid_if_entity_attributes_have_duplicate_uri() {
        NetworkConfig config = new NetworkConfig();

        Entity e = entity1();
        e.addAttribute(new Attribute("duplicateUri", "urn:column1"));
        config.addEntity(e);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Duplicate Attribute uri \"urn:column1\" found in entity \"resource1\""), validator.errors());
    }

    @Test
    public void invalid_if_entity_attributes_have_duplicate_column() {
        NetworkConfig config = new NetworkConfig();

        Entity e = entity1();
        e.addAttribute(new Attribute("column1", "urn:/column_1"));
        config.addEntity(e);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Duplicate Attribute column \"column1\" found in entity \"resource1\""), validator.errors());
    }

    @Test
    public void invalid_if_entity_attribute_uri_invalid() {
        NetworkConfig config = new NetworkConfig();

        Entity e = entity1();
        e.addAttribute(new Attribute("column6", null));
        e.addAttribute(new Attribute("column7", "some uri"));
        config.addEntity(e);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        List<String> expected = Arrays.asList(
                "Invalid Attribute uri \"null\" found in entity \"resource1\". Uri must only contain alpha-numeric or _:/ characters.",
                "Invalid Attribute uri \"some uri\" found in entity \"resource1\". Uri must only contain alpha-numeric or _:/ characters."
        );

        assertFalse(validator.isValid());
        assertEquals(expected, validator.errors());
    }

    @Test
    public void invalid_if_expeditionMetadataProperty_missing_name() {
        NetworkConfig config = new NetworkConfig();

        List<ExpeditionMetadataProperty> m = new ArrayList<>();
        m.add(new ExpeditionMetadataProperty(" ", false));
        config.setExpeditionMetadataProperties(m);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        List<String> expected = Arrays.asList("ExpeditionMetadataProperty is missing a name.");

        assertFalse(validator.isValid());
        assertEquals(expected, validator.errors());

    }

    @Test
    public void invalid_if_all_entites_on_sheet_are_hashed() {
        NetworkConfig config = new NetworkConfig();

        Entity e = entity1();
        e.setHashed(true);
        e.addRule(new RequiredValueRule(new LinkedHashSet<>(Collections.singletonList("column2")), RuleLevel.ERROR));
        config.addEntity(e);

        Entity e2 = entity3();
        e2.setWorksheet(e.getWorksheet());
        e2.setHashed(true);
        e2.addRule(new RequiredValueRule(new LinkedHashSet<>(Collections.singletonList("column1")), RuleLevel.ERROR));
        config.addEntity(e2);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        List<String> expected = Arrays.asList(
                "At least 1 entity on the worksheet: \"worksheet1\" must specify a uniqueKey and can not be a hashed entity."
        );

        assertFalse(validator.isValid());
        assertEquals(expected, validator.errors());
    }

    @Test
    public void invalid_if_duplicate_column_on_worksheet_for_unrelated_entites() {
        NetworkConfig config = new NetworkConfig();

        Entity e = entity1();
        config.addEntity(e);

        Entity e2 = entity2();
        e2.setWorksheet(e.getWorksheet());
        config.addEntity(e2);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        List<String> expected = Arrays.asList(
                "Worksheet \"worksheet1\" contains a duplicate column \"column1\"",
                "Worksheet \"worksheet1\" contains a duplicate attribute uri \"urn:column1\"",
                "Worksheet \"worksheet1\" contains a duplicate column \"column2\"",
                "Worksheet \"worksheet1\" contains a duplicate attribute uri \"urn:column2\"",
                "Worksheet \"worksheet1\" contains a duplicate column \"column3\"",
                "Worksheet \"worksheet1\" contains a duplicate attribute uri \"urn:column3\"",
                "Worksheet \"worksheet1\" contains a duplicate column \"column4\"",
                "Worksheet \"worksheet1\" contains a duplicate attribute uri \"urn:column4\""
        );

        assertFalse(validator.isValid());
        assertEquals(expected, validator.errors());
    }

    @Test
    public void invalid_if_duplicate_column_on_worksheet_for_related_entites() {
        NetworkConfig config = new NetworkConfig();

        Entity e = entity1();

        Entity e2 = entity2();
        e2.setWorksheet(e.getWorksheet());
        e2.setParentEntity(e.getConceptAlias());
        config.addEntity(e2);
        config.addEntity(e);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        List<String> expected = Arrays.asList(
                "Worksheet \"worksheet1\" contains a duplicate column \"column2\"",
                "Worksheet \"worksheet1\" contains a duplicate attribute uri \"urn:column2\"",
                "Worksheet \"worksheet1\" contains a duplicate column \"column3\"",
                "Worksheet \"worksheet1\" contains a duplicate attribute uri \"urn:column3\"",
                "Worksheet \"worksheet1\" contains a duplicate column \"column4\"",
                "Worksheet \"worksheet1\" contains a duplicate attribute uri \"urn:column4\""
        );

        assertFalse(validator.isValid());
        assertEquals(expected, validator.errors());
    }

    @Test
    public void invalid_if_hashed_entity_doesnt_require_column() {
        NetworkConfig config = new NetworkConfig();

        Entity e = entity1();
        e.setHashed(true);
        config.addEntity(e);

        Entity e2 = entity3();
        e2.setWorksheet(e.getWorksheet());
        config.addEntity(e2);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        List<String> expected = Arrays.asList(
                "Entity \"resource1\" is a hashed entity, but is missing at least 1 RequiredValueRule with level = \"ERROR\" and a column that is not the uniqueKey \"column1\""
        );

        assertFalse(validator.isValid());
        assertEquals(expected, validator.errors());
    }

    @Test
    public void invalid_if_entity_isValid_returns_false() {
        NetworkConfig config = new NetworkConfig();
        Entity e = customEntity();
        config.addEntity(e);

        NetworkConfigValidator validator = new NetworkConfigValidator(config);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Test entity validation message"), validator.errors());
    }

    private Entity entity1() {
        Entity e = new DefaultEntity("resource1", "someURI");

        e.setConceptURI(RESOURCE_URI);
        e.setUniqueKey("column1");
        e.setWorksheet("worksheet1");

        attributesList1().forEach(e::addAttribute);

        return e;
    }

    private Entity entity2() {
        Entity e = new DefaultEntity("resource2", "someURI");

        e.setConceptURI(RESOURCE_URI);
        e.setUniqueKey("column2");
        e.setWorksheet("worksheet2");

        attributesList1().forEach(e::addAttribute);

        return e;
    }

    private Entity entity3() {
        Entity e = new DefaultEntity("resource3", "someURI");

        e.setConceptURI(RESOURCE_URI);
        e.setUniqueKey("columnA");
        e.addAttribute(new Attribute("columnA", "urn:columnA"));
        e.addAttribute(new Attribute("column1", "urn:column1"));
        e.setParentEntity(entity1().getConceptAlias());

        return e;
    }

    private Entity customEntity() {
        Entity e = new DefaultEntity("resource1", "someURI") {
            @Override
            public boolean isValid(Config config) {
                Assert.notNull(config, "NetworkConfig is null");
                return false;
            }

            @Override
            public List<String> validationErrorMessages() {
                return Collections.singletonList("Test entity validation message");
            }
        };

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