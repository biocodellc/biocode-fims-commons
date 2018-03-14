package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * Select Expression. used to select additional data related to the entity
 *
 * _select_:parentEntity ->  select parentEntity (conceptAlias) data as well
 * _select_:[parent, grandParent]    ->  select parent & grandParent data. All entities must be related
 *
 * @author rjewing
 */
public class SelectExpression implements Expression {
    private String selectString;

    public SelectExpression(String selectString) {
        Assert.notNull(selectString);
        this.selectString = selectString;
    }

    public List<String> entites() {
        return Arrays.asList(selectString.split(" ?, ?"));
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "SelectExpression{" +
                "selectString='" + selectString + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectExpression)) return false;

        SelectExpression that = (SelectExpression) o;

        return selectString.equals(that.selectString);
    }

    @Override
    public int hashCode() {
        return selectString.hashCode();
    }

}
