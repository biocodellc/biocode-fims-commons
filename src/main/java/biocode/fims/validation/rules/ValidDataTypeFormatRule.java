package biocode.fims.validation.rules;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DataType;
import biocode.fims.config.models.Entity;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import biocode.fims.utils.DateUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;

import java.util.List;
import java.util.regex.Pattern;

/**
 * If a dataformat other then "string" is specified for an {@link Attribute}, we check that the data is formatted
 * correctly
 *
 * @author rjewing
 */
public class ValidDataTypeFormatRule extends AbstractRule {
    private static final String NAME = "ValidDataTypeFormat";
    private static final String GROUP_MESSAGE = "Invalid DataFormat";
    private static final Pattern INT_PATTERN = Pattern.compile("[+-]?\\d*");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("[+-]?\\d*\\.\\d*");
    private static final Pattern BOOL_PATTERN = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);

    public ValidDataTypeFormatRule() {
        super(RuleLevel.ERROR);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);
        boolean isValid = true;

        for (Record r : recordSet.recordsToPersist()) {
            for (Attribute a : recordSet.entity().getAttributes()) {

                String value = r.get(a.getUri());

                if (value.equals("")) {
                    continue;
                }

                switch (a.getDataType()) {
                    case INTEGER:
                        if (!isIntegerDataFormat(value, a.getAllowUnknown())) {
                            String msg = "\"" + a.getColumn() + "\" contains non-integer value \"" + value + "\"";
                            if (a.getAllowUnknown()) {
                                msg += ". Value can also be \"Unknown\"";
                            }
                            messages.addErrorMessage(
                                    GROUP_MESSAGE,
                                    new Message(msg)
                            );
                            isValid = false;
                            if (level().equals(RuleLevel.ERROR)) r.setError();
                        }
                        break;
                    case FLOAT:
                        if (!isFloatDataFormat(value, a.getAllowUnknown())) {
                            String msg = "\"" + a.getColumn() + "\" contains non-float value \"" + value + "\"";
                            if (a.getAllowUnknown()) {
                                msg += ". Value can also be \"Unknown\"";
                            }
                            messages.addErrorMessage(
                                    GROUP_MESSAGE,
                                    new Message(msg)
                            );
                            isValid = false;
                            if (level().equals(RuleLevel.ERROR)) r.setError();
                        }
                        break;
                    case DATE:
                    case TIME:
                    case DATETIME:
                        if (!isDateDataFormat(value, a.getDataType(), a.getDataFormat(), a.getAllowUnknown())) {
                            String msg = "\"" + a.getColumn() + "\" contains invalid date value \"" +
                                    value + "\". " + "Format must be one of [" + a.getDataFormat() + "]. If this " +
                                    "is an Excel workbook, the value can also be an Excel DATE cell";
                            if (a.getAllowUnknown()) {
                                msg += ". Value can also be \"Unknown\"";
                            }
                            messages.addErrorMessage(
                                    GROUP_MESSAGE,
                                    new Message(msg)
                            );
                            isValid = false;
                            if (level().equals(RuleLevel.ERROR)) r.setError();
                        }
                        break;
                    case BOOLEAN:
                        if (!isBooleanDataFormat(value)) {
                            messages.addErrorMessage(
                                    GROUP_MESSAGE,
                                    new Message("\"" + a.getColumn() + "\" contains non-boolean value \"" + value + "\". Must be either true or false")
                            );
                            isValid = false;
                            if (level().equals(RuleLevel.ERROR)) r.setError();
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        setError();
        return isValid;
    }

    private boolean isIntegerDataFormat(String value, boolean allowUnknown) {
        return INT_PATTERN.matcher(value).matches() || (allowUnknown && value.toLowerCase().equals("unknown"));
    }

    private boolean isFloatDataFormat(String value, boolean allowUnknown) {
        return INT_PATTERN.matcher(value).matches() || FLOAT_PATTERN.matcher(value).matches() || (allowUnknown && value.toLowerCase().equals("unknown"));
    }

    private boolean isBooleanDataFormat(String value) {
        return BOOL_PATTERN.matcher(value).matches();
    }

    private boolean isDateDataFormat(String value, DataType dataType, String dataformat, boolean allowUnknown) {
        // if the Excel cell is a DateCell, then ExcelReader will parse it as joda-time value.
        // therefore we need to add this format
        String jodaFormat;
        switch (dataType) {
            case DATE:
                jodaFormat = DateUtils.ISO_8061_DATE;
                break;
            case TIME:
                jodaFormat = DateUtils.ISO_8061_TIME;
                break;
            default:
                jodaFormat = DateUtils.ISO_8061_DATETIME;
                break;
        }
        String[] formats = (String[]) ArrayUtils.add(dataformat.split(","), jodaFormat);

        return DateUtils.isValidDateFormat(value, formats) || (allowUnknown && value.toLowerCase().equals("unknown"));
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        return true;
    }


    @Override
    public boolean mergeRule(Rule r) {
        if (!r.getClass().equals(this.getClass())) return false;

        networkRule = networkRule || r.isNetworkRule();
        return true;
    }

    @Override
    public boolean contains(Rule r) {
        if (!r.getClass().equals(this.getClass())) return false;
        return true;
    }
}
