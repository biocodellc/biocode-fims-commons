package biocode.fims.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility methods for dealing with Enums
 */
public class EnumUtils {
    private static final Logger logger = LoggerFactory.getLogger(EnumUtils.class);
    /**
     * A case-insensitive implementation of Enum.valueOf
     * @param enumeration
     * @param search
     * @param <T>
     * @return
     */
    public static <T extends Enum<T>> T lookup(@SuppressWarnings("rawtypes") Class<T> enumeration, String search) {
        for (T each: enumeration.getEnumConstants()) {
            if (each.name().compareToIgnoreCase(search) == 0) {
                return each;
            }
        }
        logger.warn("No enum value \"" + search + "\" for " + enumeration.getName());
        return null;
    }
}
