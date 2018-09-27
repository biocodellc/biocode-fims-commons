package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Expedition Search Expression
 *
 * _expeditions_:exp_1    ->  all results in expedition 'exp_1'
 * _expeditions_:[exp_1, exp_2]    ->  all results in either expedition 'exp_1' or 'exp_2'
 *
 * @author rjewing
 */
public class ExpeditionExpression implements Expression {
    private String expeditionsString;

    public ExpeditionExpression(String expeditionsString) {
        Assert.notNull(expeditionsString);
        this.expeditionsString = expeditionsString;
    }

    public List<String> expeditions() {
        return Arrays.asList(expeditionsString.replaceAll(" ", "").split(","));
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "ExpeditionExpression{" +
                "expeditionsString='" + expeditionsString + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpeditionExpression)) return false;

        ExpeditionExpression that = (ExpeditionExpression) o;

        return expeditionsString.equals(that.expeditionsString);
    }

    @Override
    public int hashCode() {
        return expeditionsString.hashCode();
    }

}
