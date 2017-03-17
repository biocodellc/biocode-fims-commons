package biocode.fims.query.dsl;

/**
 * @author rjewing
 */
public class SubQuery extends BoolQuery implements FieldQueryExpression {
    private String column;

    @Override
    public void setColumn(String column) {
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubQuery)) return false;
        if (!super.equals(o)) return false;

        SubQuery that = (SubQuery) o;

        return column != null ? column.equals(that.column) : that.column == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (column != null ? column.hashCode() : 0);
        return result;
    }
}
