package biocode.fims.projectConfig;

import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ProjectConfigTest {

    private ProjectConfig config;

    @Before
    public void setUp() {
        config = new ProjectConfig();
    }

    @Test(expected = FimsRuntimeException.class)
    public void should_throw_exception_for_non_existing_entity_when_fetching_related_entities() {
        config.addEntity(parent());
        config.getEntitiesInRelation(parent(), new Entity("notRelated"));
    }

    @Test
    public void should_return_true_for_direct_parent_child_relationship() {
        config.addEntity(parent());
        config.addEntity(child());

        assertTrue(config.areRelatedEntities("parent", "child"));
        assertTrue(config.areRelatedEntities("child", "parent"));
    }

    @Test
    public void should_determine_if_entity_is_a_child_descendant() {
        config.addEntity(parent());
        config.addEntity(child());

        assertTrue(config.isEntityChildDescendent(parent(), child()));
        assertFalse(config.isEntityChildDescendent(child(), parent()));
    }

    @Test
    public void should_return_true_for_in_direct_relationship() {
        config.addEntity(parent());
        config.addEntity(child());
        config.addEntity(grandChild());

        assertTrue(config.areRelatedEntities("parent", "grandChild"));
        assertTrue(config.areRelatedEntities("grandChild", "parent"));
    }

    @Test
    public void should_return_false_for_relationship_check_of_unknown_entity() {
        config.addEntity(parent());
        assertFalse(config.areRelatedEntities("parent", "unknown"));
    }

    @Test
    public void should_return_false_for_relationship_check_of_unrelated_entities() {
        config.addEntity(parent());
        Entity unrelated = new Entity("unrelated");
        config.addEntity(unrelated);

        assertFalse(config.areRelatedEntities("parent", "unrelated"));
    }

    @Test
    public void should_return_parent_child_entity_list_for_parent_child_relationship() {
        config.addEntity(parent());
        config.addEntity(child());

        LinkedList<Entity> result = config.getEntitiesInRelation(parent(), child());

        LinkedList<Entity> expected = new LinkedList<>();
        expected.add(parent());
        expected.add(child());

        assertEquals(expected, result);
    }

    @Test
    public void should_return_empty_list_for_unrelated_entities() {
        config.addEntity(parent());
        Entity unrelated = new Entity("unrelated");
        config.addEntity(unrelated);

        LinkedList<Entity> result = config.getEntitiesInRelation(parent(), unrelated);

        LinkedList<Entity> expected = new LinkedList<>();

        assertEquals(expected, result);
    }

    @Test
    public void should_return_parent_child_grandChild_entity_list_for_non_direct_relationship() {
        config.addEntity(grandChild());
        config.addEntity(parent());
        config.addEntity(child());

        LinkedList<Entity> result = config.getEntitiesInRelation(parent(), grandChild());

        LinkedList<Entity> expected = new LinkedList<>();
        expected.add(parent());
        expected.add(child());
        expected.add(grandChild());

        assertEquals(expected, result);
    }

    private Entity parent() {
        return new Entity("parent");
    }

    private Entity child() {
        Entity entity = new Entity("child");
        entity.setParentEntity("parent");
        return entity;
    }

    private Entity grandChild() {
        Entity entity = new Entity("grandChild");
        entity.setParentEntity("child");
        return entity;
    }
}