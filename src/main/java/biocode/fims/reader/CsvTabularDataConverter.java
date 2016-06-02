package biocode.fims.reader;

import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.reader.plugins.TabularDataReader;
import biocode.fims.settings.PathManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


/**
 * Takes a data source represented by a TabularDataReader and converts it to a
 * SQLite Database.  Each table in the source data is converted to a matching
 * table in the SQLite Database.
 */
public class CsvTabularDataConverter {
    private TabularDataReader source;
    private String outputDir;
    private String filenamePrefix;
    private File csvFile;

    /**
     * Constructs a new CsvTabularDataConverter for the specified source and
     * destination Database connection.
     *
     * @param source    A TabularDataReader with an open data source.
     * @param outputDir A valid filepath for the new csv file
     */
    public CsvTabularDataConverter(TabularDataReader source, String outputDir, String filenamePrefix) {
        this.source = source;
        this.outputDir = outputDir;
        this.filenamePrefix = filenamePrefix;
    }

    /**
     * Get the destination of the newly csv file
     *
     * @return The filepath of the csv file
     */
    public String getDestination() {
        return outputDir;
    }

    /**
     * Reads the source data and converts it to a csv file. Only the columns
     * specified in the acceptableColumns param will be written to the csv file. The data will be
     * written in the same order as the acceptableColumns list.
     */
    public void convert(List<String> acceptableColumns, String sheetName) {
        try {
            source.setTable(sheetName);
        } catch (FimsException e) {
            throw new ServerErrorException(e);
        }

        // get the columns in the order they appear in the dataset so we can refer to the columns by index later.
        // this is necessary in order to insert the column into the db in the order we expect
        List<String> datasetColumns = new ArrayList<>();

        for (String colname : source.tableGetNextRow()) {
            datasetColumns.add(colname);
        }

        // For storing data into CSV files
        StringBuffer data = new StringBuffer();
        csvFile = PathManager.createFile(filenamePrefix + ".csv", outputDir);

        try {
            FileOutputStream fos = new FileOutputStream(csvFile);
            for (int rowNum = 0; rowNum < source.getNumRows(); rowNum++) {
                String[] row = source.tableGetNextRow();

                // reorder the columns to match the same order of acceptableColumns list
                for (String col : acceptableColumns) {
                    if (datasetColumns.contains(col)) {
                        data.append(row[datasetColumns.indexOf(col)] + ",");
                    } else {
                        // if the column doesn't exist in the dataset, add a placeholder in the csv file.
                        // This is required because we later use the acceptableColumns list to insert the csv data
                        // into the db. If a column in missing from the dataset and a placeholder isn't entered in the
                        // csv, then the csv data becomes mis-aligned with acceptableColumns list, causing invalid data
                        // to be persisted
                        data.append(",");
                    }
                }

                data.append('\n');
            }

            fos.write(data.toString().getBytes());
            fos.close();

        } catch (IOException ioe) {
            throw new ServerErrorException(ioe);
        }
    }

    public File getCsvFile() {
        return csvFile;
    }
}
