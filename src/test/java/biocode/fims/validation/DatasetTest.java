package biocode.fims.validation;

import biocode.fims.config.models.DefaultEntity;
import biocode.fims.records.RecordSet;
import biocode.fims.run.Dataset;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class DatasetTest {

    @Test
    public void should_add_recordset_if_empty() {
        Dataset dataset = new Dataset();
        dataset.add(parentRecordSet());

        assertEquals("parent", dataset.get(0).conceptAlias());
    }

    @Test
    public void should_have_parents_before_children_when_inserting_children_first() {
        Dataset dataset = new Dataset();
        dataset.add(grandChildRecordSet());
        dataset.add(childRecordSet());
        dataset.add(parentRecordSet());

        assertEquals("parent", dataset.get(0).conceptAlias());
        assertEquals("child", dataset.get(1).conceptAlias());
        assertEquals("grandChild", dataset.get(2).conceptAlias());
    }

    @Test
    public void should_have_parents_before_children_when_inserting_parents_first() {
        Dataset dataset = new Dataset();
        dataset.add(parentRecordSet());
        dataset.add(childRecordSet());
        dataset.add(grandChildRecordSet());

        assertEquals("parent", dataset.get(0).conceptAlias());
        assertEquals("child", dataset.get(1).conceptAlias());
        assertEquals("grandChild", dataset.get(2).conceptAlias());
    }

    @Test
    public void should_have_parents_before_children_when_inserting_grandChild_parent_child() {
        Dataset dataset = new Dataset();
        dataset.add(grandChildRecordSet());
        dataset.add(parentRecordSet());
        dataset.add(childRecordSet());

        assertEquals("parent", dataset.get(0).conceptAlias());
        assertEquals("child", dataset.get(1).conceptAlias());
        assertEquals("grandChild", dataset.get(2).conceptAlias());
    }

    @Test
    public void should_have_non_related_before_parents_before_children() {
        Dataset dataset = new Dataset();
        dataset.add(grandChildRecordSet());
        dataset.add(parentRecordSet());
        dataset.add(childRecordSet());
        dataset.add(nonRelatedRecordSet());

        assertEquals("nonRelated", dataset.get(0).conceptAlias());
        assertEquals("parent", dataset.get(1).conceptAlias());
        assertEquals("child", dataset.get(2).conceptAlias());
        assertEquals("grandChild", dataset.get(3).conceptAlias());
    }

    private RecordSet grandChildRecordSet() {
        RecordSet grandChild = new RecordSet(new DefaultEntity("grandChild", "someURI"), false);
        grandChild.setParent(childRecordSet());
        return grandChild;
    }

    private RecordSet childRecordSet() {
        RecordSet child = new RecordSet(new DefaultEntity("child", "someURI"), false);
        child.setParent(parentRecordSet());
        return child;
    }

    private RecordSet parentRecordSet() {
        return new RecordSet(new DefaultEntity("parent", "someURI"), false);
    }

    private RecordSet nonRelatedRecordSet() {
        return new RecordSet(new DefaultEntity("nonRelated", "someURI"), false);
    }

}