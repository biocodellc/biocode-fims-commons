package biocode.fims.reader;

import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.settings.PathManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


/**
 * Takes a data source represented by a TabularDataReader and converts it to
 * JSON.
 */
public class JSONTabularDataConverter {
    private TabularDataReader source;

    /**
     * Constructs a new JSONTabularDataConverter for the specified source
     *
     * @param source A TabularDataReader with an open data source.
     */
    public JSONTabularDataConverter(TabularDataReader source) {
        this.source = source;
    }

    /**
     * Reads the source data and converts it to a {@link JSONArray}. Only the columns
     * specified in the acceptableColumns param will be written to each {@link JSONObject}. The data will be
     * written in the same order as the acceptableColumns list.
     *
     * @param acceptableColumns
     * @param sheetName
     * @return {@link JSONArray} containing the dataset rows as {@JSONObject}s
     */
    public JSONArray convert(List<String> acceptableColumns, String sheetName) {
        try {
            source.setTable(sheetName);
        } catch (FimsException e) {
            throw new ServerErrorException(e);
        }

        JSONArray dataset = new JSONArray();

        // get the columns in the order they appear in the dataset so we can refer to the columns by index later.
        List<String> datasetColumns = new ArrayList<>();
        Collections.addAll(datasetColumns, source.tableGetNextRow());

        for (int rowNum = 0; rowNum < source.getNumRows(); rowNum++) {
            String[] row = source.tableGetNextRow();
            JSONObject sample = new JSONObject();

            // reorder the columns to match the same order of acceptableColumns list
            for (String col : acceptableColumns) {
                if (datasetColumns.contains(col)) {
                    sample.put(col, row[datasetColumns.indexOf(col)]);
                }
            }
            dataset.add(sample);

        }
        return dataset;
    }
}
