package biocode.fims.validation.rules;

import biocode.fims.projectConfig.models.Entity;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Check that minimum/maximum numbers are entered correctly.
 * <p>
 * minimumColumn and maximumColumn must both set
 * <p>
 * 1) check that either both minimumColumn and maximumColumn are either present or missing, but not only 1 column present
 * 2) check that each column is a valid number
 * 3) check that maximumColumn is greater then the minimumColumn for each {@link Record}
 *
 * @author rjewing
 */
public class MinMaxNumberRule extends AbstractRule {
    private static final String NAME = "MinMaxNumber";
    private static final String GROUP_MESSAGE = "Number outside of range";
    private static final Pattern pattern = Pattern.compile("(\\d+.?\\d*|.\\d+)");

    @JsonProperty
    private String minimumColumn;
    @JsonProperty
    private String maximumColumn;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private MinMaxNumberRule() {
    }

    public MinMaxNumberRule(String minimumColumn, String maximumColumn, RuleLevel level) {
        super(level);
        this.minimumColumn = minimumColumn;
        this.maximumColumn = maximumColumn;
    }

    public MinMaxNumberRule(String minimumColumn, String maximumColumn) {
        this(minimumColumn, maximumColumn, RuleLevel.WARNING);
    }


    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);
        boolean isValid = true;

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String minUri = recordSet.entity().getAttributeUri(minimumColumn);
        String maxUri = recordSet.entity().getAttributeUri(maximumColumn);

        for (Record r : recordSet.records()) {

            String minColVal = r.get(minUri);
            String maxColVal = r.get(maxUri);

            if (minColVal.equals("") && maxColVal.equals("")) {
                continue;
            } else if (minColVal.equals("")) {
                isValid = false;
                messages.addWarningMessage(
                        "Spreadsheet check",
                        new Message("Column \"" + maximumColumn + "\" exists but must have corresponding column \"" + minimumColumn + "\"")
                );
                continue;
            } else if (maxColVal.equals("")) {
                isValid = false;
                messages.addWarningMessage(
                        "Spreadsheet check",
                        new Message("Column \"" + minimumColumn + "\" exists but must have corresponding column \"" + maximumColumn + "\"")
                );
                continue;
            }

            boolean validNumbers = true;
            if (!pattern.matcher(minColVal).matches()) {
                messages.addMessage(
                        GROUP_MESSAGE,
                        new Message("non-numeric value \"" + minColVal + "\" for column \"" + minimumColumn + "\""),
                        level()
                );
                validNumbers = false;
                isValid = false;
            }

            if (!pattern.matcher(maxColVal).matches()) {
                messages.addMessage(
                        GROUP_MESSAGE,
                        new Message("non-numeric value \"" + maxColVal + "\" for column \"" + maximumColumn + "\""),
                        level()
                );
                validNumbers = false;
                isValid = false;
            }

            if (validNumbers) {
                try {
                    if (Double.parseDouble(minColVal) > Double.parseDouble(maxColVal)) {
                        messages.addMessage(
                                GROUP_MESSAGE,
                                new Message("Illegal values! " + minimumColumn + " = " + minColVal + " while " + maximumColumn + " = " + maxColVal + ""),
                                level()
                        );
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    messages.addMessage(
                            GROUP_MESSAGE,
                            new Message("could not determine if \"" + minColVal + "\" is greater then \"" + maxColVal + "\". Are they both numbers?"),
                            level()
                    );
                }
            }

        }

        return isValid;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        if (StringUtils.isBlank(minimumColumn) || StringUtils.isBlank(maximumColumn)) {
            messages.add(
                    "Invalid MinMaxNumber Rule configuration. minimumColumn and maximumColumn must not be null or empty"
            );

            return false;
        }

        return entityHasAttribute(messages, entity, minimumColumn)
                && entityHasAttribute(messages, entity, maximumColumn);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MinMaxNumberRule)) return false;
        if (!super.equals(o)) return false;

        MinMaxNumberRule that = (MinMaxNumberRule) o;

        if (minimumColumn != null ? !minimumColumn.equals(that.minimumColumn) : that.minimumColumn != null)
            return false;
        return maximumColumn != null ? maximumColumn.equals(that.maximumColumn) : that.maximumColumn == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (minimumColumn != null ? minimumColumn.hashCode() : 0);
        result = 31 * result + (maximumColumn != null ? maximumColumn.hashCode() : 0);
        return result;
    }
}
