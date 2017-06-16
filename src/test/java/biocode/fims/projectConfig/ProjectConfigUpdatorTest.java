package biocode.fims.projectConfig;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.models.Project;
import biocode.fims.repositories.RecordRepository;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ProjectConfigUpdatorTest {

    @Test
    public void should_track_new_and_removed_entities() {
        ProjectConfig updatedConfig = new ProjectConfig();
        updatedConfig.addEntity(entity1());

        ProjectConfig origConfig = new ProjectConfig();
        origConfig.addEntity(entity2());

        ProjectConfigUpdator updator = new ProjectConfigUpdator(updatedConfig);
        ProjectConfig result = updator.update(origConfig);

        assertEquals(updatedConfig, result);
        assertEquals(Collections.singletonList(entity1()), updator.newEntities());
        assertEquals(Collections.singletonList(entity2()), updator.removedEntities());
    }

    @Test
    public void should_ignore_updates_to_entity_uniqueKey_and_parentEntity() {
        ProjectConfig updatedConfig = new ProjectConfig();
        updatedConfig.addEntity(entity1());
        Entity updatedEntity = entity2();
        updatedEntity.setUniqueKey("new Column");
        updatedEntity.setParentEntity("new parent");
        updatedConfig.addEntity(updatedEntity);

        ProjectConfig origConfig = new ProjectConfig();
        origConfig.addEntity(entity1());
        origConfig.addEntity(entity2());

        ProjectConfigUpdator updator = new ProjectConfigUpdator(updatedConfig);
        ProjectConfig result = updator.update(origConfig);

        assertEquals(origConfig, result);
        assertEquals(Collections.emptyList(), updator.newEntities());
        assertEquals(Collections.emptyList(), updator.removedEntities());
    }

    private Entity entity1() {
        Entity e = new Entity("resource1", "someURI");

        e.setConceptForwardingAddress("http://example.com");
        e.setUniqueKey("column1");
        e.setWorksheet("worksheet1");

        e.addAttribute(new Attribute("column1", "resource1_column1"));

        return e;
    }

    private Entity entity2() {
        Entity e = new Entity("resource2", "someURI");
        e.setUniqueKey("column2");
        e.setParentEntity("resource1");

        e.addAttribute(new Attribute("column2", "resource2_column1"));
        e.addAttribute(new Attribute("parent", "resource1_column1"));

        return e;
    }

}