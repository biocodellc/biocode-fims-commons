package biocode.fims.authorizers;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author rjewing
 */
public enum ExpeditionVisibility {
    ANYONE("anyone"), PROJECT("project members"), EXPEDITION("expedition members");

    private String name;

    ExpeditionVisibility(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
