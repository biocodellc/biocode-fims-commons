package biocode.fims.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Utils class for working with dates
 */
public class DateUtils {
    /**
     * Checks to see if a string is a valid date
     * @param s
     * @return
     */
    public static boolean isValidDateFormat(String s, String[] formats) {
        boolean validDate = false;
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setLenient(false);

        for (String format: formats) {
            try {
                sdf.applyPattern(format);
                sdf.parse(s.trim());
                validDate = true;
            } catch (ParseException e) {
            }
        }
        return validDate;
    }
}
