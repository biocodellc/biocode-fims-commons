package biocode.fims.query;

import biocode.fims.query.dsl.ExistsExpression;
import biocode.fims.query.dsl.ExpeditionExpression;
import biocode.fims.query.dsl.LogicalExpression;
import biocode.fims.query.dsl.LogicalOperator;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ExpeditionCollectingExpressionVisitorTest {

    @Test
    public void should_return_empty_set_if_not_expedition_expressions() {
        ExpeditionCollectingExpressionVisitor collector = new ExpeditionCollectingExpressionVisitor();

        ExistsExpression expression = new ExistsExpression("");
        expression.accept(collector);

        assertTrue(collector.expeditions().size() == 0);
    }

    @Test
    public void should_return_all_unique_expeditions_in_an_expression_tree() {
        ExpeditionCollectingExpressionVisitor collector = new ExpeditionCollectingExpressionVisitor();

        ExistsExpression e1 = new ExistsExpression("");
        ExpeditionExpression e2 = new ExpeditionExpression("demo");

        LogicalExpression l1 = new LogicalExpression(LogicalOperator.AND, e1, e2);
        ExpeditionExpression e3 = new ExpeditionExpression("demo, another, test");
        LogicalExpression expression = new LogicalExpression(LogicalOperator.OR, l1, e3);
        expression.accept(collector);

        Set<String> expected = new HashSet<>();
        expected.add("demo");
        expected.add("another");
        expected.add("test");

        assertEquals(expected, collector.expeditions());
    }

}