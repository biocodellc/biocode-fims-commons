package biocode.fims.reader;

import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.reader.plugins.TabularDataReader;

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
    private String dest;

    /**
     * Constructs a new CsvTabularDataConverter for the specified source and
     * destination Database connection.
     *
     * @param source A TabularDataReader with an open data source.
     * @param dest   A valid filepath for the new csv file
     */
    public CsvTabularDataConverter(TabularDataReader source, String dest) {
        this.source = source;
        this.dest = dest;
    }

    /**
     * Get the destination of the newly csv file
     *
     * @return The filepath of the csv file
     */
    public String getDestination() {
        return dest;
    }

    /**
     * Reads the source data and converts it to a csv file.
     */
    public void convert() {
        convert(source.getColNames());
    }

    /**
     * Reads the source data and converts it to a csv file. Only the columns
     * specified in the acceptableColumns param will be written to the csv file.
     */
    public void convert(List<String> acceptableColumns) {
        int colcnt = 0;

        // keep track of any columns with missing colNames to skip data later on
        List<Integer> skipColumns = new ArrayList<>();

        for (String colname : source.tableGetNextRow()) {
            // keep track of undefined columns
            if (!acceptableColumns.contains(colname)) {
                skipColumns.add(colcnt);
            }
            colcnt++;
        }

        // For storing data into CSV files
        StringBuffer data = new StringBuffer();
        File csvFile = new File(dest);

        try {
            FileOutputStream fos = new FileOutputStream(csvFile);
            while (source.tableHasNextRow()) {
                String[] row = source.tableGetNextRow();

                for (int col=0; col < colcnt; col++) {
                    if (!skipColumns.contains(col)) {
                        data.append(row[col] + ",");
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
}
