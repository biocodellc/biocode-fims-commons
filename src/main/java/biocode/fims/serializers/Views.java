package biocode.fims.serializers;

/**
 * @author RJ Ewing
 */
public class Views {

    public static class Summary {}
    public static class Public extends Summary {}
    public static class Detailed extends Public {}
    public static class DetailedConfig extends Detailed {}
}
