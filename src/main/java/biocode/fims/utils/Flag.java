package biocode.fims.utils;

/**
 * Helper class for including flag @QueryParam in REST requests
 *
 * @author RJ Ewing
 */
public class Flag {
    private final boolean  isPresent;
    public Flag(String param) { isPresent = (param != null && !param.equalsIgnoreCase("false")); }
    public boolean isPresent() { return isPresent; }
}
