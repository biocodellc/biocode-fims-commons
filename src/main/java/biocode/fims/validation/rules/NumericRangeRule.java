package biocode.fims.validation.rules;

import biocode.fims.config.models.Entity;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Checks for valid numeric values against the range string.
 * <p>
 * The range can contain multiple ranges separate by "|" like:  ">=-90|<=90"
 * or, simply ">=0"
 *
 * @author rjewing
 */
public class NumericRangeRule extends SingleColumnRule {
    private static final String NAME = "NumericRange";
    private static final String GROUP_MESSAGE = "Invalid number format";
    @JsonProperty
    private String range;

    private boolean validRange = true;
    private List<Range> ranges;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private NumericRangeRule() {
    }

    public NumericRangeRule(String column, String range, RuleLevel level) {
        super(column, level);
        this.range = range;
    }

    public NumericRangeRule(String column, String range) {
        this(column, range, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);
        boolean allowUnknown = recordSet.entity().getAttribute(column).getAllowUnknown();

        List<String> invalidValues = new ArrayList<>();

        for (Record r : recordSet.recordsToPersist()) {

            String value = r.get(uri);

            if (!value.equals("")) {
                try {
                    Double numericValue = Double.parseDouble(value);

                    for (Range range : ranges) {

                        switch (range.operator) {
                            case GREATER_THEN_EQUALS:
                                if (numericValue < range.value) {
                                    invalidValues.add(value);
                                }
                                break;
                            case GREATER_THEN:
                                if (numericValue <= range.value) {
                                    invalidValues.add(value);
                                }
                                break;
                            case LESS_THEN_EQUALS:
                                if (numericValue > range.value) {
                                    invalidValues.add(value);
                                }
                                break;
                            case LESS_THEN:
                                if (numericValue >= range.value) {
                                    invalidValues.add(value);
                                }
                                break;
                            default:
                                invalidValues.add(value);
                        }
                    }
                } catch (NumberFormatException e) {
                    if (allowUnknown && value.toLowerCase().equals("unknown")) {
                        // unknown is a valid value
                    } else {
                        invalidValues.add(value);
                    }
                }
            }

        }

        if (invalidValues.size() == 0) {
            return true;
        }

        setMessages(invalidValues, messages);
        setError();
        return false;
    }

    private void setMessages(List<String> invalidValues, EntityMessages messages) {
        for (String value: invalidValues) {
            messages.addMessage(
                    GROUP_MESSAGE,
                    new Message(
                            "Value \"" + value + "\" out of range for \"" + column + "\" using range validation = \"" + range + "\""
                    ),
                    level()
            );
        }
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        boolean valid = super.validConfiguration(messages, entity);

        if (StringUtils.isEmpty(range)) {
            messages.add("Invalid " + name() + " Rule configuration. range must not be blank or null.");

            return false;
        }

        parseRange();
        if (!validRange) {
            messages.add("Invalid " + name() + " Rule configuration. Could not parse range \"" + range + "\"");

            return false;
        }

        return valid;
    }

    @Override
    public String name() {
        return NAME;
    }


    private void parseRange() {
        ranges = new ArrayList<>();
        List<String> rangeStrings = Arrays.asList(range.split("\\|"));

        for (String rs : rangeStrings) {
            rs = rs.trim();

            try {
                if (rs.startsWith(">=")) {
                    ranges.add(
                            new Range(OPERATOR.GREATER_THEN_EQUALS, rs.substring(2))
                    );
                } else if (rs.startsWith("<=")) {
                    ranges.add(
                            new Range(OPERATOR.LESS_THEN_EQUALS, rs.substring(2))
                    );
                } else if (rs.startsWith(">")) {
                    ranges.add(
                            new Range(OPERATOR.GREATER_THEN, rs.substring(1))
                    );
                } else if (rs.startsWith("<")) {
                    ranges.add(
                            new Range(OPERATOR.LESS_THEN, rs.substring(1))
                    );
                } else {
                    validRange = false;
                }
            } catch (NumberFormatException e) {
                validRange = false;
            }

        }
    }

    private static class Range {
        private OPERATOR operator;
        private Double value;

        Range(OPERATOR operator, String value) {
            this.operator = operator;
            this.value = Double.parseDouble(value);
        }
    }

    private enum OPERATOR {
        GREATER_THEN, LESS_THEN, GREATER_THEN_EQUALS, LESS_THEN_EQUALS
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumericRangeRule)) return false;
        if (!super.equals(o)) return false;

        NumericRangeRule that = (NumericRangeRule) o;

        return range != null ? range.equals(that.range) : that.range == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (range != null ? range.hashCode() : 0);
        return result;
    }
}
