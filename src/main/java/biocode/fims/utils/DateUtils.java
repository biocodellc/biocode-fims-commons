package biocode.fims.utils;

import biocode.fims.digester.DataType;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.apache.commons.lang.ArrayUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utils class for working with dates
 */
public class DateUtils {
    public static String ISO_8061_DATETIME = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static String ISO_8061_DATE = "yyyy-MM-dd";
    public static String ISO_8061_TIME = "HH:mm:ss.SSS";

    /**
     * Checks to see if a string is a valid date
     *
     * @param s
     * @return
     */
    public static boolean isValidDateFormat(String s, String[] formats) {
        for (String format : formats) {
            if (dateStringMatchesFormat(s, format)) {
                return true;
            }
        }

        return false;
    }

    /**
     * convert dateString into a valid ISO8601 value
     * @param dateString the string to convert
     * @param formats possible formats for the dateString
     * @param dataType {@link DataType} to use for the conversion. Either DATE, TIME, or DATETIME
     * @return
     */
    public static String convertToISO8601(String dateString, String[] formats, DataType dataType) {
        DateTimeFormatter formatter;

        switch (dataType) {
            case DATE:
                formats = (String[]) ArrayUtils.add(formats, ISO_8061_DATE);
                for (String format : formats) {
                    if (dateStringMatchesFormat(dateString, format)) {
                        formatter = DateTimeFormat.forPattern(format);
                        return formatter.parseLocalDate(dateString.trim()).toString();
                    }
                }
                break;
            case DATETIME:
                formats = (String[]) ArrayUtils.add(formats, ISO_8061_DATETIME);
                for (String format : formats) {
                    if (dateStringMatchesFormat(dateString, format)) {
                        formatter = DateTimeFormat.forPattern(format);
                        return formatter.parseLocalDateTime(dateString.trim()).toString();
                    }
                }
                break;
            case TIME:
                formats = (String[]) ArrayUtils.add(formats, ISO_8061_TIME);
                for (String format : formats) {
                    if (dateStringMatchesFormat(dateString, format)) {
                        formatter = DateTimeFormat.forPattern(format);
                        return formatter.parseLocalTime(dateString.trim()).toString();
                    }
                }
                break;
            default:
                throw new FimsRuntimeException("Couldn't detect date format for value: " + dateString, 500);

        }

        throw new FimsRuntimeException("", "Invalid Datatype", 500);
    }

    private static boolean dateStringMatchesFormat(String dateString, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setLenient(false);

        try {
            sdf.applyPattern(format);
            Date date = sdf.parse(dateString.trim());
            if (dateString.trim().equals(sdf.format(date))) {
                return true;
            }
        } catch (ParseException e) {
        }
        return false;
    }
}
