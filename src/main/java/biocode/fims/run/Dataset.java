package biocode.fims.run;

import biocode.fims.models.records.RecordSet;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is a sorted collection of {@link biocode.fims.models.records.RecordSet}s so that RecordSets w/o parent are
 * always before child RecordSets
 *
 * @author rjewing
 */
public class Dataset implements Iterable<RecordSet> {
    private LinkedList<RecordSet> recordSets;

    public Dataset() {
        this.recordSets = new LinkedList<>();
    }

    public Dataset(List<RecordSet> recordSets) {
        this();

        for (RecordSet r : recordSets) {
            add(r);
        }
    }

    /**
     * adds RecordSets so that any recordSet w/o a parent in inserted at the beginning of the list. If the
     * recordSet has a parent, it is inserted after the parent, but before any children
     *
     * @param recordSet
     */
    public void add(RecordSet recordSet) {
        ListIterator<RecordSet> itr = recordSets.listIterator();

        if (!recordSet.hasParent()) {
            itr.add(recordSet);
            return;
        }

        while (itr.hasNext()) {
            RecordSet elementInList = itr.next();
            RecordSet parent = elementInList.parent();

            if (parent != null &&
                    (!recordSet.hasParent() ||
                            elementInList.parent().conceptAlias().equals(recordSet.conceptAlias()))) {

                itr.previous();
                itr.add(recordSet);
                return;
            }
        }

        itr.add(recordSet);
    }

    public RecordSet get(int index) {
        return recordSets.get(index);
    }

    public int size() {
        return recordSets.size();
    }

    @Override
    public Iterator<RecordSet> iterator() {
        return recordSets.iterator();
    }

    @Override
    public void forEach(Consumer<? super RecordSet> action) {
        recordSets.forEach(action);
    }

    @Override
    public Spliterator<RecordSet> spliterator() {
        return recordSets.spliterator();
    }

    public Stream<RecordSet> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<RecordSet> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dataset)) return false;

        Dataset that = (Dataset) o;

        return recordSets.equals(that.recordSets);
    }

    @Override
    public int hashCode() {
        return recordSets.hashCode();
    }
}
