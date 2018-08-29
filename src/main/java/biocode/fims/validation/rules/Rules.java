package biocode.fims.validation.rules;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * @author rjewing
 */
public class Rules implements Iterable<Rule> {
    private LinkedHashSet<Rule> rules;

    public Rules() {
        rules = new LinkedHashSet<>();
    }

    public LinkedHashSet<Rule> get() {
        return rules;
    }

    public void add(Rule rule) {
        mergeRule(rule);
    }

    public void addAll(Collection<Rule> rules) {
        rules.forEach(this::mergeRule);
    }

    private void mergeRule(Rule r) {
        Iterator<Rule> it = rules.iterator();
        while (it.hasNext()) {
            Rule rule = it.next();

            if (rule.getClass().equals(r.getClass())) {
                if (rule.mergeRule(r)) return;
                else if (rule.equals(r)) {
                    rule.setNetworkRule(rule.isNetworkRule() || r.isNetworkRule());
                    return;
                }
            }
        }

        // didn't find a rule to merge with
        rules.add(r);
    }

    @Override
    public Iterator<Rule> iterator() {
        return rules.iterator();
    }

    @Override
    public void forEach(Consumer<? super Rule> action) {
        rules.forEach(action);
    }

    @Override
    public Spliterator<Rule> spliterator() {
        return rules.spliterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rules)) return false;

        Rules rules1 = (Rules) o;

        return rules.equals(rules1.rules);
    }

    @Override
    public int hashCode() {
        return rules.hashCode();
    }
}
