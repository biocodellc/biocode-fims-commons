package biocode.fims.query.dsl;

import org.springframework.util.Assert;

/**
 * Range Search Expression
 *
 * TODO only valid on integer, float, date, datetime, and time attributes
 * TODO need to validate range
 *
 * col1:[1 TO 10]       ->      >= 1 AND <= 10
 * col1:[1 TO 10}       ->      >= 1 AND < 10
 * col1:{1 TO 10}       ->      > 1 AND < 10
 * col1:{* TO 100]      ->      <= 100
 *
 * @author rjewing
 */
public class RangeExpression implements Expression {
    private String column;
    private final String range;

    public RangeExpression(String column, String range) {
        Assert.notNull(range);
        Assert.notNull(column);
        this.column = column;
        this.range = range;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RangeExpression)) return false;

        RangeExpression that = (RangeExpression) o;

        if (column != null ? !column.equals(that.column) : that.column != null) return false;
        return range.equals(that.range);
    }

    @Override
    public int hashCode() {
        int result = column != null ? column.hashCode() : 0;
        result = 31 * result + range.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RangeExpression{" +
                "column='" + column + '\'' +
                ", range='" + range + '\'' +
                '}';
    }
}
