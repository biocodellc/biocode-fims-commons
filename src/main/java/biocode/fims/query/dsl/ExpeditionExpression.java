package biocode.fims.query.dsl;

import org.springframework.util.Assert;

/**
 * Expedition Search Expression
 *
 * _expedition_:exp_1    ->  all results in expedition 'exp_1'
 * _expedition_:[exp_1, exp_2]    ->  all results in either expedition 'exp_1' or 'exp_2'
 *
 * @author rjewing
 */
public class ExpeditionExpression implements Expression {
    private String expeditionsString;

    public ExpeditionExpression(String expeditionsString) {
        Assert.notNull(expeditionsString);
        this.expeditionsString = expeditionsString;
    }

    @Override
    public String toString() {
        return "ExistsExpression{" +
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
