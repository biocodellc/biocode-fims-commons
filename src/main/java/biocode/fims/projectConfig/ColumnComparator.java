package biocode.fims.projectConfig;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.validation.rules.RuleLevel;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author rjewing
 */
public class ColumnComparator implements Comparator<String> {
    private final ProjectConfig config;
    private final LinkedList<String> sortedColumns;

    public ColumnComparator(ProjectConfig config, String worksheet) {
        this.config = config;
        this.sortedColumns = sortColumns(worksheet);
    }

    @Override
    public int compare(String a, String b) {
        return indexOf(a).compareTo(indexOf(b));
    }

    private Integer indexOf(String col) {
        int index = 0;

        for (String column : sortedColumns) {
            if (column.equals(col)) return index;
            index++;
        }
        return -1;
    }

    private LinkedList<String> sortColumns(String worksheet) {
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        Set<String> requiredColumns = config.getRequiredColumns(worksheet, RuleLevel.ERROR);
        Set<String> warningColumns = config.getRequiredColumns(worksheet, RuleLevel.WARNING);

        // required columns first
        for (String c : requiredColumns) {
            columns.add(c);
        }
        // then warning columns
        for (String c : warningColumns) {
            columns.add(c);
        }
        // then by column order in config
        for (Attribute a : config.attributesForSheet(worksheet)) {
            columns.add(a.getColumn());
        }

        return new LinkedList<>(columns);
    }
}
