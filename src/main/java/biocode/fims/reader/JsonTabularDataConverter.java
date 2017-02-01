package biocode.fims.reader;

import biocode.fims.digester.Attribute;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.rest.SpringObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Takes a data source represented by a TabularDataReader and converts it to JSON
 */
public class JsonTabularDataConverter {
    private TabularDataReader source;

    public JsonTabularDataConverter(TabularDataReader source) {
        this.source = source;
    }

    /**
     * Reads the source data and converts it to a JSONArray.
     */
    public ArrayNode convert(List<Attribute> attributes, String sheetName) {
        ArrayNode sheet = new SpringObjectMapper().createArrayNode();
        source.setTable(sheetName);

        if (!source.tableHasNextRow()) {
            throw new FimsRuntimeException(ValidationCode.NO_DATA, 400);
        }

        // get the columns in the order they appear in the table so we can refer to the columns by index later.
        // this is necessary in order to insert the column into the db in the order we expect
        // NOTE: JBD-- commenting this line out and replacing with getColNames below... not sure why tableColumns
        // were being populated with tableGetNextRow() and why it was working previously??
        // List<String> tableColumns = Arrays.asList(source.tableGetNextRow());
        List<String> tableColumns = source.getColNames();
        List<String> attributeColumns = new ArrayList<>();

        for (Attribute a : attributes) {
            attributeColumns.add(a.getColumn());
        }

        for (int rowNum = 0; rowNum < source.getNumRows(); rowNum++) {
            ObjectNode resource = sheet.addObject();
            String[] row = source.tableGetNextRow();
           // String[] row = source.tableGetNextRow();

            for (int col = 0; col < tableColumns.size(); col++) {
                String column = tableColumns.get(col);
                if (resource.has(column)) {
                    throw new FimsRuntimeException(ValidationCode.DUPLICATE_COLUMNS, 400, column);
                }
                resource.put(column, row[col]);
            }
        }

        if (sheet.size() == 0) {
            throw new FimsRuntimeException(ValidationCode.EMPTY_DATASET, 400);
        }

        return sheet;
    }
}
