package biocode.fims.query.dsl;

import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.query.ExpressionVisitor;
import org.springframework.util.Assert;

/**
 * Range Search Expression
 *
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


    public String column() {
        return column;
    }

    public ParsedRange parsedRange() {
        return new ParsedRange(range);
    }

    @Override
    public void accept(ExpressionVisitor visitor) {
        visitor.visit(this);
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


    public static class ParsedRange {
        private ComparisonOperator leftOperator;
        private String leftValue;
        private ComparisonOperator rightOperator;
        private String rightValue;

        public ParsedRange(String range) {
            parse(range);
        }

        public ComparisonOperator leftOperator() {
            return leftOperator;
        }

        public String leftValue() {
            return leftValue;
        }

        public ComparisonOperator rightOperator() {
            return rightOperator;
        }

        public String rightValue() {
            return rightValue;
        }

        private void parse(String rangeString) {
            String[] range = rangeString.split(" ?TO ?");

            if (range.length != 2) {
                throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, "invalid range " + rangeString);
            }

            String left = range[0].trim();
            String right = range[1].trim();

            try {
                parseLeft(left);
                parseRight(right);
            } catch (FimsException e) {
                throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, "invalid range " + rangeString);
            }

            if (leftValue == null && rightValue == null) {
                throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, "invalid range " + rangeString);
            }
        }

        private void parseLeft(String value) throws FimsException {
            if (value.startsWith("[")) {
                leftOperator = ComparisonOperator.GREATER_THEN_EQUAL;
            } else if (value.startsWith("{")) {
                leftOperator = ComparisonOperator.GREATER_THEN;
            } else {
                throw new FimsException();
            }

            leftValue = value.substring(1);

            if (leftValue.equals("*")) {
                leftValue = null;
                leftOperator = null;
            }
        }

        private void parseRight(String value) throws FimsException {
            if (value.endsWith("]")) {
                rightOperator = ComparisonOperator.LESS_THEN_EQUAL;
            } else if (value.endsWith("}")) {
                rightOperator = ComparisonOperator.LESS_THEN;
            } else {
                throw new FimsException();
            }

            rightValue = value.substring(0, value.length() - 1);

            if (rightValue.equals("*")) {
                rightValue = null;
                rightOperator = null;
            }
        }
    }
}
