package biocode.fims.query;

/**
 * @author rjewing
 */
public enum QueryConstants {
    ROOT_IDENTIFIER("rootIdentifier"),
    DATA("data"),
    EXPEDITION_CODE("expeditionCode"),
    PROJECT_ID("projectId");

    private final String name;

    QueryConstants(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
