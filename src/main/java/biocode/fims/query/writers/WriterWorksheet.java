package biocode.fims.query.writers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class WriterWorksheet {
    public final String sheetName;
    public final List<String> columns;
    public final List<Map<String, Object>> data;

    public WriterWorksheet(String sheetName, List<String> columns) {
        this(sheetName, columns, Collections.emptyList());
    }

    public WriterWorksheet(String sheetName, List<String> columns, List<Map<String, Object>> data) {
        this.sheetName = sheetName;
        this.columns = columns;
        this.data = data;
    }
}
