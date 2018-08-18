package biocode.fims.query;

import biocode.fims.records.RecordSources;
import biocode.fims.projectConfig.models.Entity;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author rjewing
 */
public class QueryResults implements Iterable<QueryResult> {

    private final List<QueryResult> results;

    public QueryResults(List<QueryResult> results) {
        this.results = results;
    }

    public List<QueryResult> results() {
        return results;
    }

    public List<Entity> entities() {
        return results.stream()
                .map(QueryResult::entity)
                .collect(Collectors.toList());
    }

    public QueryResult getResult(String conceptAlias) {
        return results.stream()
                .filter(r -> r.entity().getConceptAlias().equals(conceptAlias))
                .findFirst()
                .orElse(null);
    }

    public boolean isEmpty() {
        return results.stream()
                .noneMatch(r -> r.get(false).size() > 0);
    }

    public Map<String, List<Map<String, String>>> toMap(boolean includeEmpty, RecordSources sources) {
        Map<String, List<Map<String, String>>> map = new HashMap<>();

        for (QueryResult result : results) {
            String conceptAlias = result.entity().getConceptAlias();
            map.put(conceptAlias, result.get(includeEmpty, sources.get(conceptAlias)));
        }

        return map;
    }

    public void sort(Comparator<? super QueryResult> comparator) {
        results.sort(comparator);
    }

    @Override
    public Iterator<QueryResult> iterator() {
        return results.iterator();
    }

    @Override
    public void forEach(Consumer<? super QueryResult> action) {
        results.forEach(action);
    }

    @Override
    public Spliterator<QueryResult> spliterator() {
        return results.spliterator();
    }

    public Stream<QueryResult> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<QueryResult> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }


    /**
     * Comparator to sort child entities before parents by worksheet
     */
    public static class ChildrenFirstComparator implements Comparator<QueryResult> {
        @Override
        public int compare(QueryResult a, QueryResult b) {
            Entity e1 = a.entity();
            Entity e2 = b.entity();

            if (!Objects.equals(e1.getWorksheet(), e2.getWorksheet())) return 0;
            if (e1.isChildEntity() && e2.getConceptAlias().equals(e1.getParentEntity())) return -1;
            if (e2.isChildEntity() && e1.getConceptAlias().equals(e1.getParentEntity())) return 1;

            return 0;
        }
    }
}
