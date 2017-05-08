package biocode.fims.validation.rules;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import biocode.fims.utils.DateUtils;
import org.apache.commons.lang.ArrayUtils;
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

    ValidDataTypeFormatRule() {
        super(RuleLevel.ERROR);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);
        boolean isValid = true;

        for (Record r : recordSet.records()) {
            for (Attribute a : recordSet.entity().getAttributes()) {

                String value = r.get(a.getUri());

                if (value.equals("")) {
                    continue;
                }

                switch (a.getDatatype()) {
                    case INTEGER:
                        if (!isIntegerDataFormat(value)) {
                            messages.addErrorMessage(
                                    GROUP_MESSAGE,
                                    new SimpleMessage("\"" + a.getColumn() + "\" contains non-integer value \"" + value + "\"")
                            );
                            isValid = false;
                        }
                        break;
                    case FLOAT:
                        if (!isFloatDataFormat(value)) {
                            messages.addErrorMessage(
                                    GROUP_MESSAGE,
                                    new SimpleMessage("\"" + a.getColumn() + "\" contains non-float value \"" + value + "\"")
                            );
                            isValid = false;
                        }
                        break;
                    case DATE:
                    case TIME:
                    case DATETIME:
                        if (!isDateDataFormat(value, a.getDatatype(), a.getDataformat())) {
                            messages.addErrorMessage(
                                    GROUP_MESSAGE,
                                    new SimpleMessage("\"" + a.getColumn() + "\" contains invalid date value \"" +
                                            value + "\". " + "Format must be one of [" + a.getDataformat() + "]. If this " +
                                            "is an Excel workbook, the value can also be an Excel DATE cell")
                            );
                            isValid = false;
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

    private boolean isIntegerDataFormat(String value) {
        return INT_PATTERN.matcher(value).matches();
    }

    private boolean isFloatDataFormat(String value) {
        return FLOAT_PATTERN.matcher(value).matches();
    }

    private boolean isDateDataFormat(String value, DataType dataType, String dataformat) {
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

        return DateUtils.isValidDateFormat(value, formats);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        return true;
    }
}
