package biocode.fims.reader;

import biocode.fims.digester.Attribute;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ValidationCode;
import biocode.fims.reader.plugins.TabularDataReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Takes a data source represented by a TabularDataReader and converts it to dataset
 */
public class DatasetTabularDataConverter {
    private TabularDataReader source;

    public DatasetTabularDataConverter(TabularDataReader source) {
        this.source = source;
    }

    /**
     * Reads the source data and converts it to a dataset.
     */
    public JSONArray convert(List<Attribute> attributes, String sheetName) {
        JSONArray sheet = new JSONArray();
        source.setTable(sheetName);

        if (!source.tableHasNextRow()) {
            throw new FimsRuntimeException(ValidationCode.NO_DATA, 400);
        }

        // get the columns in the order they appear in the dataset so we can refer to the columns by index later.
        // this is necessary in order to insert the column into the db in the order we expect
        List<String> datasetColumns = Arrays.asList(source.tableGetNextRow());
        List<String> attributeColumns = new ArrayList<>();

        for (Attribute a : attributes) {
            attributeColumns.add(a.getColumn());
        }

        for (int rowNum = 0; rowNum < source.getNumRows(); rowNum++) {
            JSONObject sample = new JSONObject();
            String[] row = source.tableGetNextRow();

            for (int col = 0; col < datasetColumns.size(); col++) {
                String column = datasetColumns.get(col);
                if (sample.containsKey(column)) {
                    throw new FimsRuntimeException(ValidationCode.DUPLICATE_COLUMNS, 400, column);
                }
                sample.put(column, row[col]);
            }
            sheet.add(sample);
        }

        if (sheet.size() == 0) {
            throw new FimsRuntimeException(ValidationCode.EMPTY_DATASET, 400);
        }

        return sheet;
    }
}
