package biocode.fims.query;

import biocode.fims.digester.Entity;

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
}
