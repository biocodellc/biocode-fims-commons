package biocode.fims.query.dsl;

import org.springframework.util.Assert;

/**
 * Exists Search Expression
 *
 * _exists_:col1    ->  data ? 'col1'
 * _exists_:[col1, col2]    ->  data ?& array['col1', 'col2']
 *
 * @author rjewing
 */
public class ExistsExpression implements Expression {
    private String columnString;

    public ExistsExpression(String columnString) {
        Assert.notNull(columnString);
        this.columnString = columnString;
    }

    @Override
    public String toString() {
        return "ExistsExpression{" +
                "columnString='" + columnString + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExistsExpression)) return false;

        ExistsExpression that = (ExistsExpression) o;

        return columnString.equals(that.columnString);
    }

    @Override
    public int hashCode() {
        return columnString.hashCode();
    }

}
