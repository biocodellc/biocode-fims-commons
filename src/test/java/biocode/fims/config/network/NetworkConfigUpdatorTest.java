package biocode.fims.config.network;

import biocode.fims.config.models.DefaultEntity;
import biocode.fims.config.models.NetworkEntity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import org.junit.Test;

import java.util.Collections;

import static biocode.fims.fimsExceptions.errorCodes.ConfigCode.MISSING_ATTRIBUTE;
import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class NetworkConfigUpdatorTest {

    @Test
    public void should_track_new_and_removed_entities() {
        NetworkConfig updatedConfig = new NetworkConfig();
        updatedConfig.addEntity(entity1());

        NetworkConfig origConfig = new NetworkConfig();
        origConfig.addEntity(entity2());

        NetworkConfigUpdator updator = new NetworkConfigUpdator(updatedConfig);
        NetworkConfig result = updator.update(origConfig);

        assertEquals(updatedConfig, result);
        assertEquals(Collections.singletonList(new NetworkEntity(entity1())), updator.newEntities());
        assertEquals(Collections.singletonList(new NetworkEntity(entity2())), updator.removedEntities());
    }

    @Test
    public void should_ignore_updates_to_entity_uniqueKey_and_parentEntity() {
        NetworkConfig updatedConfig = new NetworkConfig();
        updatedConfig.addEntity(entity1());
        Entity updatedEntity = entity2();
        updatedEntity.setUniqueKey("new Column");
        updatedEntity.setParentEntity("new parent");
        updatedConfig.addEntity(updatedEntity);

        NetworkConfig origConfig = new NetworkConfig();
        origConfig.addEntity(entity1());
        origConfig.addEntity(entity2());

        NetworkConfigUpdator updator = new NetworkConfigUpdator(updatedConfig);
        NetworkConfig result = updator.update(origConfig);

        assertEquals(origConfig, result);
        assertEquals(Collections.emptyList(), updator.newEntities());
        assertEquals(Collections.emptyList(), updator.removedEntities());
    }

    @Test
    public void should_ignore_updates_to_attribute_uri() {
        NetworkConfig updatedConfig = new NetworkConfig();
        Entity e = entity1();
        e.getAttributes().get(0).setUri("new_uri");
        updatedConfig.addEntity(e);

        NetworkConfig origConfig = new NetworkConfig();
        origConfig.addEntity(entity1());

        NetworkConfigUpdator updator = new NetworkConfigUpdator(updatedConfig);
        NetworkConfig result = updator.update(origConfig);

        assertEquals(origConfig, result);
        assertEquals(Collections.emptyList(), updator.newEntities());
        assertEquals(Collections.emptyList(), updator.removedEntities());
    }

    @Test
    public void should_ignore_updates_to_unique_key_attribute_column() {
        NetworkConfig updatedConfig = new NetworkConfig();
        Entity e = entity1();
        e.getAttributeByUri(e.getUniqueKeyURI()).setColumn("new_column_name");
        updatedConfig.addEntity(e);

        NetworkConfig origConfig = new NetworkConfig();
        origConfig.addEntity(entity1());

        NetworkConfigUpdator updator = new NetworkConfigUpdator(updatedConfig);
        NetworkConfig result = updator.update(origConfig);

        Entity updatedEntity = result.entities().get(0);

        try {
            updatedEntity.getAttribute(updatedEntity.getUniqueKey());
        } catch (FimsRuntimeException err) {
            if (err.getErrorCode().equals(MISSING_ATTRIBUTE)) {
                // Can't find any attribute w/ column == updatedEntity.uniqueKey
                assert false;
            }
            throw err;
        }

        String column = updatedEntity.getAttributeByUri(updatedEntity.getUniqueKeyURI()).getColumn();

        assertEquals(origConfig.entities().get(0).getUniqueKey(), column);
        assertEquals(Collections.emptyList(), updator.newEntities());
        assertEquals(Collections.emptyList(), updator.removedEntities());
    }

    private Entity entity1() {
        Entity e = new DefaultEntity("resource1", "someURI");

        e.setUniqueKey("column1");
        e.setWorksheet("worksheet1");

        e.addAttribute(new Attribute("column1", "resource1_column1"));

        return e;
    }

    private Entity entity2() {
        Entity e = new DefaultEntity("resource2", "someURI");
        e.setUniqueKey("column2");
        e.setParentEntity("resource1");

        e.addAttribute(new Attribute("column2", "resource2_column1"));
        e.addAttribute(new Attribute("parent", "resource1_column1"));

        return e;
    }

}