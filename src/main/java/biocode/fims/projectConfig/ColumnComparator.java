package biocode.fims.projectConfig;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.validation.rules.RuleLevel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class ColumnComparator implements Comparator<String> {
    private final ProjectConfig config;
    private final LinkedList<String> sortedColumns;
    private final List<String> sheetColumns;
    private final String worksheet;

    public ColumnComparator(ProjectConfig config, String worksheet) {
        this.config = config;
        this.worksheet = worksheet;
        this.sheetColumns = config.attributesForSheet(worksheet).stream()
                .map(Attribute::getColumn)
                .collect(Collectors.toList());
        this.sortedColumns = sortColumns();
    }

    @Override
    public int compare(String a, String b) {
        if (sortedColumns.contains(a)) {
            if (sortedColumns.contains(b)) {
                return indexOf(sortedColumns, a).compareTo(indexOf(sortedColumns, b));
            }

            return -1;
        } else if (sortedColumns.contains(b)) {
            return 1;
        }

        return a.compareTo(b);
    }

    private Integer indexOf(List<String> collection, String col) {
        int index = 0;

        for (String column : collection) {
            if (column.equals(col)) return index;
            index++;
        }
        return -1;
    }

    private LinkedList<String> sortColumns() {
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        LinkedList<String> requiredColumns = getRequiredColumns(RuleLevel.ERROR);
        LinkedList<String> warningColumns = getRequiredColumns(RuleLevel.WARNING);

        // required columns first
        columns.addAll(requiredColumns);
        // then warning columns
        columns.addAll(warningColumns);
        // then by column order in config
        columns.addAll(sheetColumns);

        return new LinkedList<>(columns);
    }

    private LinkedList<String> getRequiredColumns(RuleLevel level) {
        LinkedList<String> requiredColumns = new LinkedList<>(config.getRequiredColumns(worksheet, level));
        requiredColumns.sort(Comparator.comparing(a -> indexOf(sheetColumns, a)));
        return requiredColumns;
    }
}
