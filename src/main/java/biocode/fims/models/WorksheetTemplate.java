package biocode.fims.models;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

/**
 * @author rjewing
 */
public class WorksheetTemplate {
    public final String name;
    public final List<String> columns;

    @JsonCreator
    public WorksheetTemplate(String name, List<String> columns) {
        this.name = name;
        this.columns = columns;
    }
}
