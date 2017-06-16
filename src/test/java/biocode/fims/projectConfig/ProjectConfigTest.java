package biocode.fims.projectConfig;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
        config.entitiesInRelation(parent(), new Entity("notRelated", "someURI"));
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
        Entity unrelated = new Entity("unrelated", "someURI");
        config.addEntity(unrelated);

        assertFalse(config.areRelatedEntities("parent", "unrelated"));
    }

    @Test
    public void should_return_parent_child_entity_list_for_parent_child_relationship() {
        config.addEntity(parent());
        config.addEntity(child());

        LinkedList<Entity> result = config.entitiesInRelation(parent(), child());

        LinkedList<Entity> expected = new LinkedList<>();
        expected.add(parent());
        expected.add(child());

        assertEquals(expected, result);
    }

    @Test
    public void should_return_empty_list_for_unrelated_entities() {
        config.addEntity(parent());
        Entity unrelated = new Entity("unrelated", "someURI");
        config.addEntity(unrelated);

        LinkedList<Entity> result = config.entitiesInRelation(parent(), unrelated);

        LinkedList<Entity> expected = new LinkedList<>();

        assertEquals(expected, result);
    }

    @Test
    public void should_return_parent_child_grandChild_entity_list_for_non_direct_relationship() {
        config.addEntity(grandChild());
        config.addEntity(parent());
        config.addEntity(child());

        LinkedList<Entity> result = config.entitiesInRelation(parent(), grandChild());

        LinkedList<Entity> expected = new LinkedList<>();
        expected.add(parent());
        expected.add(child());
        expected.add(grandChild());

        assertEquals(expected, result);
    }

    @Test
    public void should_generate_normalized_attribute_uris() {
        Entity entity = parent();
        entity.addAttribute(new Attribute("column", "parent_column"));

        Attribute a1 = new Attribute();
        a1.setColumn("Space Column");
        entity.addAttribute(a1);

        Attribute a2 = new Attribute();
        a2.setColumn("@Column");
        entity.addAttribute(a2);

        Attribute a3 = new Attribute();
        a3.setColumn("Column");
        entity.addAttribute(a3);

        config.addEntity(entity);
        config.generateUris();

        Entity e = config.entities().get(0);

        assertEquals("parent_space_column", e.getAttribute("Space Column").getUri());
        assertEquals("parent_column1", e.getAttribute("@Column").getUri());
        assertEquals("parent_column2", e.getAttribute("Column").getUri());
    }

    @Test
    public void should_return_false_if_not_valid() {
        Entity entity = parent();
        entity.addAttribute(new Attribute("column", null));

        config.addEntity(entity);

        assertFalse(config.isValid());

        assertEquals(Collections.singletonList("Invalid Attribute uri \"null\" found in entity \"parent\". Uri must only contain alpha-numeric or _:/ characters."), config.errors());
    }

    private Entity parent() {
        return new Entity("parent", "someURI");
    }

    private Entity child() {
        Entity entity = new Entity("child", "someURI");
        entity.setParentEntity("parent");
        return entity;
    }

    private Entity grandChild() {
        Entity entity = new Entity("grandChild", "someURI");
        entity.setParentEntity("child");
        return entity;
    }
}