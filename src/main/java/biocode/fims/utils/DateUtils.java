package biocode.fims.utils;

import biocode.fims.digester.DataType;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utils class for working with dates
 */
public class DateUtils {
    public static String ISO_8061_DATETIME = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static String ISO_8061_DATE = "yyyy-MM-dd";
    public static String ISO_8061_TIME = "HH:mm:ss.SSS";

    private static String[] ISO8601_DATE_FORMATS = {
            "YYYY",
            "YYYY-MM",
            "YYYY-MM-DD",
            "YYYYMMDD"
    };
    private static String[] ISO8601_TIME_FORMATS = {
            "hh:mm:ss.sss",
            "hhmmss.sss",
            "hh:mm:ss",
            "hhmmss",
            "hh:mm",
            "hhmm",
            "hh"
    };

    /**
     * Check to see if a format is a valid ISO8601 format
     *
     * @param format
     * @param dataType
     * @return
     */
    public static boolean isValidISO8601DateFormat(String format, DataType dataType) {
        switch (dataType) {
            case DATE:
                for (String f : ISO8601_DATE_FORMATS) {
                    if (StringUtils.equals(f, format)) {
                        return true;
                    }
                }
                break;
            case TIME:
                for (String f : ISO8601_TIME_FORMATS) {
                    if (StringUtils.equals(f, format)) {
                        return true;
                    }
                }
                break;
            case DATETIME:
                String dateFormat = format.split("T")[0];
                String timeFormat = format.split("T")[1];

                for (String f : ISO8601_DATE_FORMATS) {
                    if (StringUtils.equals(f, dateFormat)) {
                        for (String tf : ISO8601_TIME_FORMATS) {
                            if (StringUtils.equals(tf, timeFormat)) {
                                return true;
                            }
                        }
                    }
                }
                break;
        }

        return false;
    }

    /**
     * Checks to see if a date string matches one of the given formats
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
     * convert dateString to a given format
     *
     * @param dateString the string to convert
     * @param newFormat  the format you would like to convert the date to
     * @param formats    possible formats for the dateString
     * @return
     */
    public static String convertDateToFormat(String dateString, String newFormat, String[] formats) {
        DateTimeFormatter formatter;
        formats = (String[]) ArrayUtils.add(formats, ISO_8061_DATE);
        formats = (String[]) ArrayUtils.add(formats, ISO_8061_DATETIME);
        formats = (String[]) ArrayUtils.add(formats, ISO_8061_TIME);

        for (String format : formats) {
            if (dateStringMatchesFormat(dateString, format)) {
                formatter = DateTimeFormat.forPattern(format);
                return formatter.parseDateTime(dateString.trim()).toString(newFormat);
            }
        }

        throw new FimsRuntimeException("Couldn't detect date format for value: " + dateString, 500);
    }

    private static boolean dateStringMatchesFormat(String dateString, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormat.forPattern(format);
            formatter.parseDateTime(dateString);
        } catch (IllegalArgumentException|UnsupportedOperationException e) {
            return false;
        }
        return true;
    }
}
