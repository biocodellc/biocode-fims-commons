package biocode.fims.query.dsl;

/**
 * @author rjewing
 */
public enum ComparisonOperator {
    EQUALS("="), LESS_THEN("<"), GREATER_THEN(">"), LESS_THEN_EQUAL("<="), GREATER_THEN_EQUAL(">="), NOT_EQUALS("<>");

    private final String operator;

    ComparisonOperator(String operator) {
        this.operator = operator;
    }

    public static ComparisonOperator fromOp(String operator) {
        for (ComparisonOperator comparisonOperator: values()) {
            if (comparisonOperator.operator.equalsIgnoreCase(operator)) {
                return comparisonOperator;
            }
        }
        throw new IllegalArgumentException("Invalid ComparisonOperator: " + operator);
    }


    @Override
    public String toString() {
        return operator;
    }
}
