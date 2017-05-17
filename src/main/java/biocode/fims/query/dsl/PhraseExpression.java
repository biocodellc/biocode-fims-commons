package biocode.fims.query.dsl;

import org.springframework.util.Assert;

/**
 * Case Insensitive Phrase Search Expression
 *
 * @author rjewing
 */
public class PhraseExpression implements Expression {
    private String column;
    private final String term;

    public PhraseExpression(String column, String term) {
        Assert.notNull(term);
        Assert.notNull(column);
        this.column = column;
        this.term = term;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhraseExpression)) return false;

        PhraseExpression that = (PhraseExpression) o;

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
        return "PhraseExpression{" +
                "column='" + column + '\'' +
                ", term='" + term + '\'' +
                '}';
    }
}
