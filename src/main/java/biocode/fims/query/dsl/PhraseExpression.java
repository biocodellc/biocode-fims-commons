package biocode.fims.query.dsl;

import biocode.fims.query.ExpressionVisitor;
import org.springframework.util.Assert;

/**
 * Case Insensitive Phrase Search Expression
 *
 * @author rjewing
 */
public class PhraseExpression implements Expression {
    private final String column;
    private final String phrase;

    public PhraseExpression(String column, String phrase) {
        Assert.notNull(phrase);
        Assert.notNull(column);
        this.column = column;
        this.phrase = phrase;
    }

    public String column() {
        return column;
    }

    public String phrase() {
        return phrase;
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhraseExpression)) return false;

        PhraseExpression that = (PhraseExpression) o;

        if (column != null ? !column.equals(that.column) : that.column != null) return false;
        return phrase.equals(that.phrase);
    }

    @Override
    public int hashCode() {
        int result = column != null ? column.hashCode() : 0;
        result = 31 * result + phrase.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PhraseExpression{" +
                "column='" + column + '\'' +
                ", phrase='" + phrase + '\'' +
                '}';
    }
}
