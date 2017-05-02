package biocode.fims.models.records;

import java.util.List;

/**
 * @author rjewing
 */
public interface Record {
    String get(String property);

    boolean has(String property);

    void set(String property, String value);

    List<Record> all();
}
