package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;
import org.springframework.util.Assert;

/**
 * Full Text Search Expression.
 * <p>
 * column is optional if term is a single word
 *
 * TODO: Also supports multi term queries. The default operator for multi term queries is &. If
 *
 * col1:value               ->  fts for "value"
 * col1:val*                ->  prefix matching
 * TODO:co1:(value1 | !value2)   -> fts for "value1 | !value2
 * TODO:co1:(value1  value2)   -> fts for "value1 & value2
 *
 * @author rjewing
 */
public class FTSExpression implements Expression {
    private final String column;
    private final String term;

    public FTSExpression(String column, String term) {
        Assert.notNull(term);
        this.column = column;
        this.term = term;
    }

    public String column() {
        return column;
    }

    public String term() {
        return term;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FTSExpression)) return false;

        FTSExpression that = (FTSExpression) o;

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
        return "FTSExpression{" +
                "column='" + column + '\'' +
                ", term='" + term + '\'' +
                '}';
    }
}
