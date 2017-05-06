package biocode.fims.validation.rules;

import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.springframework.util.Assert;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Check that minimum/maximum numbers are entered correctly.
 *
 * 1) check that either both columns are present or missing, but not only 1 column present
 * 2) check that each column is a valid number
 * 3) check that the 2nd column in columns list is greater the the 1st column
 *
 * @author rjewing
 */
public class MinMaxNumberRule extends MultiColumnRule {
    private static final String NAME = "MinMaxNumber";
    private static final String GROUP_MESSAGE = "Number outside of range";
    private static final Pattern pattern = Pattern.compile("(\\d+.?\\d*|.\\d+)");

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);
        boolean isValid = true;

        if (!twoColumnsSpecified()) {
            messages.addErrorMessage(
                    "Invalid Rule Configuration",
                    new SimpleMessage(
                            "Invalid MinMaxNumber Rule configuration. 2 columns should be specified, " +
                                    "but we found " + columns.size() + ". Talk to your project administrator to fix the issue"
                    )
            );

            hasError = true;
            return false;
        }

        LinkedList<String> uris = getColumnUris(recordSet.entity());

        for (Record r : recordSet.records()) {

            String minColVal = r.get(uris.get(0));
            String maxColVal = r.get(uris.get(1));

            if (minColVal.equals("") && maxColVal.equals("")) {
                continue;
            } else if (minColVal.equals("")) {
                isValid = false;
                messages.addWarningMessage(
                        "Spreadsheet check",
                        new SimpleMessage("Column \"" + columns.get(1) + "\" exists but must have corresponding column \"" + columns.get(0) + "\"")
                );
                continue;
            } else if (maxColVal.equals("")) {
                isValid = false;
                messages.addWarningMessage(
                        "Spreadsheet check",
                        new SimpleMessage("Column \"" + columns.get(0) + "\" exists but must have corresponding column \"" + columns.get(1) + "\"")
                );
                continue;
            }

            boolean validNumbers = true;
            if (!pattern.matcher(minColVal).matches()) {
                messages.addMessage(
                        GROUP_MESSAGE,
                        new SimpleMessage("non-numeric value \"" + minColVal + "\" for column \"" + columns.get(0) + "\""),
                        level()
                );
                validNumbers = false;
                isValid = false;
            }

            if (!pattern.matcher(maxColVal).matches()) {
                messages.addMessage(
                        GROUP_MESSAGE,
                        new SimpleMessage("non-numeric value \"" + maxColVal + "\" for column \"" + columns.get(1) + "\""),
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
                                new SimpleMessage("Illegal values! " + columns.get(0) + " = " + minColVal + " while " + columns.get(1) + " = " + maxColVal + ""),
                                level()
                        );
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    messages.addMessage(
                            GROUP_MESSAGE,
                            new SimpleMessage("could not determine if \"" + minColVal + "\" is greater then \"" + maxColVal + "\". Are they both numbers?"),
                            level()
                    );
                }
            }

        }

        return isValid;
    }

    private boolean twoColumnsSpecified() {
        return columns.size() == 2;
    }

    @Override
    public boolean validConfiguration(List<String> messages) {
        if (!twoColumnsSpecified()) {
            messages.add(
                    "Invalid MinMaxNumber Rule configuration. 2 columns should be specified, " +
                            "but we found " + columns.size() + ". Talk to your project administrator to fix the issue"
            );

            return false;
        }

        return super.validConfiguration(messages);
    }

    @Override
    public String name() {
        return NAME;
    }

}
