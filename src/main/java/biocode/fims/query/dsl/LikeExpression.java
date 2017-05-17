package biocode.fims.query.dsl;

import org.springframework.util.Assert;

/**
 * ILike Search Expression
 *
 * col1:"%value"    ->  col1 ILIKE '%value'
 *
 * @author rjewing
 */
public class LikeExpression implements Expression {
    private String column;
    private final String term;

    public LikeExpression(String column, String term) {
        Assert.notNull(term);
        Assert.notNull(column);
        this.column = column;
        this.term = term;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LikeExpression)) return false;

        LikeExpression that = (LikeExpression) o;

        if (column != null ? !column.equals(that.column) : that.column != null) return false;
        return term.equals(that.term);
    }

    @Override
    public int hashCode() {
        int result = column != null ? column.hashCode() : 0;
        result = 31 * result + term.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LikeExpression{" +
                "column='" + column + '\'' +
                ", term='" + term + '\'' +
                '}';
    }
}
