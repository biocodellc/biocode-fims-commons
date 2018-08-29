package biocode.fims.config.project;

import biocode.fims.config.models.*;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.models.ExpeditionMetadataProperty;
import biocode.fims.records.Record;
import biocode.fims.validation.rules.RequiredValueRule;
import biocode.fims.validation.rules.RuleLevel;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ProjectConfigValidatorTest {

    @Test(expected = IllegalArgumentException.class)
    public void invalid_if_no_config() {
        new ProjectConfigValidator(null, networkConfig());
    }

    @Test
    public void invalid_if_list_not_in_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.entities().addAll(networkConfig.entities());
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        config.addList(new List());

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Project config validation lists differ from the network config validation lists"), validator.errors());
    }

    @Test
    public void invalid_if_network_expedition_prop_not_in_config() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.entities().addAll(networkConfig.entities());
        config.addList(networkConfig.lists().get(0));

        config.addExpeditionMetadataProperty(new ExpeditionMetadataProperty("some other prop", false));

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Project config expeditionMetadataProperties is missing a network prop: \"prop1\""), validator.errors());
    }

    @Test
    public void invalid_if_entity_not_in_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addEntity(new DefaultEntity("testing", "testingUri"));
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"testing\" is not a registered entity for this network"), validator.errors());
    }

    @Test
    public void invalid_if_entity_conceptUri_differs_from_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));


        Entity networkEntity = networkConfig.entity("sample");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), "some other uri");
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.setUniqueKey(networkEntity.getUniqueKey());
        e.setParentEntity(networkEntity.getParentEntity());
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"sample\".conceptUri does not match the registered entity's conceptUri"), validator.errors());
    }

    @Test
    public void invalid_if_entity_parentEntity_differs_from_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));


        Entity networkEntity = networkConfig.entity("sample");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.setUniqueKey(networkEntity.getUniqueKey());
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"sample\".parentEntity does not match the registered entity's parentEntity"), validator.errors());
    }

    @Test
    public void invalid_if_entity_recordType_differs_from_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));


        Entity networkEntity = networkConfig.entity("sample");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.setUniqueKey(networkEntity.getUniqueKey());
        e.setParentEntity(networkEntity.getParentEntity());
        e.setRecordType(Record.class);
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"sample\".recordType does not match the registered entity's recordType"), validator.errors());
    }

    @Test
    public void invalid_if_entity_attribute_not_in_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        Entity networkEntity = networkConfig.entity("sample");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.setUniqueKey(networkEntity.getUniqueKey());
        e.setParentEntity(networkEntity.getParentEntity());

        e.addAttribute(new Attribute("nonNetwork", "nonNetworkUri"));
        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"sample\" contains an Attribute \"nonNetworkUri\" that is not found in the network entity"), validator.errors());
    }

    @Test
    public void invalid_if_entity_attribute_column_differs_from_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        Entity networkEntity = networkConfig.entity("event");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.addAttribute(networkEntity.getAttribute("recordedBy"));
        e.addRules(networkEntity.getRules());
        e.setUniqueKey(networkEntity.getUniqueKey());

        Attribute a = new Attribute("year2", "urn:year");
        a.setDataType(DataType.DATE);
        a.setDataFormat("yyyy");
        e.addAttribute(a);

        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"event\" contains an Attribute \"urn:year\" whos column does not match the network Attribute's column"), validator.errors());
    }


    @Test
    public void invalid_if_entity_attribute_dataType_differs_from_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        Entity networkEntity = networkConfig.entity("event");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.addAttribute(networkEntity.getAttribute("recordedBy"));
        e.addRules(networkEntity.getRules());
        e.setUniqueKey(networkEntity.getUniqueKey());

        Attribute a = new Attribute("year", "urn:year");
        a.setDataType(DataType.STRING);
        a.setDataFormat("yyyy");
        e.addAttribute(a);

        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"event\" contains an Attribute \"urn:year\" whos dataType does not match the network Attribute's dataType"), validator.errors());
    }

    @Test
    public void invalid_if_entity_attribute_dataFormat_differs_from_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        Entity networkEntity = networkConfig.entity("event");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.addAttribute(networkEntity.getAttribute("recordedBy"));
        e.addRules(networkEntity.getRules());
        e.setUniqueKey(networkEntity.getUniqueKey());

        Attribute a = new Attribute("year", "urn:year");
        a.setDataType(DataType.DATE);
        a.setDataFormat("yy");
        e.addAttribute(a);

        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"event\" contains an Attribute \"urn:year\" whos dataFormat does not match the network Attribute's dataFormat"), validator.errors());
    }

    @Test
    public void invalid_if_entity_attribute_internal_differs_from_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        Entity networkEntity = networkConfig.entity("event");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.addAttribute(networkEntity.getAttribute("recordedBy"));
        e.addRules(networkEntity.getRules());
        e.setUniqueKey(networkEntity.getUniqueKey());

        Attribute a = new Attribute("year", "urn:year");
        a.setDataType(DataType.DATE);
        a.setDataFormat("yyyy");
        a.setInternal(true);
        e.addAttribute(a);

        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"event\" contains an Attribute \"urn:year\" whos internal property does not match the network Attribute's internal property"), validator.errors());
    }

    @Test
    public void invalid_if_entity_attribute_definedBy_differs_from_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        Entity networkEntity = networkConfig.entity("event");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.addRules(networkEntity.getRules());
        e.setUniqueKey(networkEntity.getUniqueKey());

        Attribute a = new Attribute("recordedBy", "urn:recordedBy");
        a.setDelimitedBy(",");
        a.setDefinedBy("officialDefinition2");
        e.addAttribute(a);

        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"event\" contains an Attribute \"urn:recordedBy\" whos definedBy does not match the network Attribute's definedBy"), validator.errors());
    }

    @Test
    public void invalid_if_entity_attribute_delimitedBy_differs_from_network() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        Entity networkEntity = networkConfig.entity("event");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.addRules(networkEntity.getRules());
        e.setUniqueKey(networkEntity.getUniqueKey());

        Attribute a = new Attribute("recordedBy", "urn:recordedBy");
        a.setDelimitedBy(";");
        a.setDefinedBy("officialDefinition");
        e.addAttribute(a);

        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"event\" contains an Attribute \"urn:recordedBy\" whos delimitedBy does not match the network Attribute's delimitedBy"), validator.errors());
    }

    @Test
    public void invalid_if_entity_missing_network_rule() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        Entity networkEntity = networkConfig.entity("event");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.addAttribute(networkEntity.getAttribute("recordedBy"));
        e.setUniqueKey(networkEntity.getUniqueKey());

        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"event\" is missing a network Rule: type: \"RequiredValue\", level: \"ERROR\""), validator.errors());
    }

    @Test
    public void invalid_if_entity_has_invalid_unique_key() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        config.addEntity(networkConfig.entity("event"));

        Entity networkEntity = networkConfig.entity("sample");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.addAttribute(networkEntity.getAttribute("sampleID"));
        e.addAttribute(networkEntity.getAttribute("eventID"));
        e.setParentEntity(networkEntity.getParentEntity());

        config.addEntity(e);

        // network uniqueKey is okay
        e.setUniqueKey(networkEntity.getUniqueKey());
        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);
        assertTrue(validator.isValid());

        // parentEntity uniqueKey is okay
        e.setUniqueKey(networkConfig.entity("event").getUniqueKey());
        validator = new ProjectConfigValidator(config, networkConfig);
        assertTrue(validator.isValid());

        // hashed entity w/o uniqueKey is okay
        e.setUniqueKey(networkEntity.getUniqueKey());
        Entity event = config.entity("event");
        config.entities().remove(event);
        Entity newEvent = new DefaultEntity(event.getConceptAlias(), event.getConceptURI());
        newEvent.addAttribute(event.getAttribute(event.getUniqueKey()));
        newEvent.addAttribute(event.getAttribute("recordedBy"));
        newEvent.addRules(event.getRules());
        newEvent.setHashed(true);
        config.addEntity(newEvent);
        validator = new ProjectConfigValidator(config, networkConfig);
        assertTrue(validator.isValid());

        // this is invalid
        newEvent.setHashed(false);
        newEvent.setUniqueKey("recordedBy");
        validator = new ProjectConfigValidator(config, networkConfig);

        assertFalse(validator.isValid());
        assertEquals(Arrays.asList("Entity \"event\" does not specify a valid uniqueKey. The uniqueKey can be the network entity's uniqueKey or a parent entity's uniqueKey"), validator.errors());
    }


    @Test
    public void should_be_valid_if_network_rule_is_merged() {
        NetworkConfig networkConfig = networkConfig();

        ProjectConfig config = new ProjectConfig();
        config.addList(networkConfig.lists().get(0));
        config.addExpeditionMetadataProperty(networkConfig.expeditionMetadataProperties().get(0));

        Entity networkEntity = networkConfig.entity("event");
        Entity e = new DefaultEntity(networkEntity.getConceptAlias(), networkEntity.getConceptURI());
        e.addAttribute(networkEntity.getAttribute(networkEntity.getUniqueKey()));
        e.addAttribute(networkEntity.getAttribute("recordedBy"));
        e.addAttribute(networkEntity.getAttribute("year"));
        e.setUniqueKey(networkEntity.getUniqueKey());

        // network rule
        e.addRule(new RequiredValueRule(new LinkedHashSet<>(Arrays.asList("recordedBy")), RuleLevel.ERROR));
        // this rule will be merged w/ above
        e.addRule(new RequiredValueRule(new LinkedHashSet<>(Arrays.asList("year")), RuleLevel.ERROR));

        config.addEntity(e);

        ProjectConfigValidator validator = new ProjectConfigValidator(config, networkConfig);

        assertTrue(validator.isValid());
    }

    private NetworkConfig networkConfig() {
        NetworkConfig c = new NetworkConfig();

        Entity event = new DefaultEntity("event", "eventURI");

        Attribute a = new Attribute("eventID", "urn:eventID");
        event.addAttribute(a);

        Attribute a2 = new Attribute("recordedBy", "urn:recordedBy");
        a2.setDelimitedBy(",");
        a2.setDefinition("some def");
        a2.setDefinedBy("officialDefinition");
        event.addAttribute(a2);

        Attribute a3 = new Attribute("year", "urn:year");
        a3.setDataFormat("yyyy");
        a3.setDataType(DataType.DATE);
        event.addAttribute(a3);

        event.addRule(new RequiredValueRule(new LinkedHashSet<>(Arrays.asList("recordedBy")), RuleLevel.ERROR));
        event.setUniqueKey("eventID");
        event.setWorksheet("Events");

        c.addEntity(event);

        Entity sample = new DefaultEntity("sample", "sampleURI");
        sample.addAttribute(a);

        Attribute sa = new Attribute("sampleID", "urn:sampleID");
        sample.addAttribute(sa);

        sample.setParentEntity(event.getConceptAlias());
        sample.setUniqueKey("sampleID");

        c.addEntity(sample);

        List l = new List();
        l.setAlias("list1");
        l.setCaseInsensitive(true);
        Field f = new Field();
        f.setUri("uri1");
        f.setValue("val1");
        l.addField(f);
        c.addList(l);

        ExpeditionMetadataProperty prop1 = new ExpeditionMetadataProperty("prop1", true);
        prop1.setNetworkProp(true);
        c.addExpeditionMetadataProperty(prop1);

        return c;
    }

}