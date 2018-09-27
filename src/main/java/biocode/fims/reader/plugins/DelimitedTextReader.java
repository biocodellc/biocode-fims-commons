package biocode.fims.reader.plugins;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.records.RecordMetadata;
import biocode.fims.config.project.ProjectConfig;
import com.opencsv.*;
import com.opencsv.CSVReader;

import java.io.*;
import java.util.*;

/**
 * Provides the ability to parse delimited text files.
 * <p>
 * 1) each record is expected to be on a single line;
 * 2) fields may be enclosed in double quotes;
 * 3) double quotes inside of a double-quoted string should be escaped with a double quote;
 * 4) space characters are not trimmed from the beginning or end of fields.
 * <p>
 * If a field is double-quoted, there should be no additional characters outside of the double quotes.
 * Records must not span multiple lines.
 * Standard escape sequences (e.g., "\"\t") are also supported inside of quoted fields.
 * Blank lines are ignored.
 * <p>
 * Finally, note that this class expects text to consist of simple ASCII
 * (1 byte) characters, and that characters 0-8 and 10-31 are all treated as
 * empty whitespace and ignored (unless they occur within a quoted string).
 * <p>
 * <p>
 * This Reader expects the following RecordMetadata:
 * <p>
 * - sheetName
 */
public abstract class DelimitedTextReader extends AbstractTabularDataReader {
    public static final String SHEET_NAME_KEY = "sheetName";

    // FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF).
    private static final String UTF8_BOM = "\uFEFF";

    private String sheetName;
    protected CSVReader reader;
    private Iterator<String[]> it;
    private char delimiter;

    DelimitedTextReader(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata, char delimiter) {
        super(file, projectConfig, recordMetadata);
        this.delimiter = delimiter;

        if (!recordMetadata.has(SHEET_NAME_KEY)) {
            throw new FimsRuntimeException(DataReaderCode.MISSING_METADATA, 500);
        }

        this.sheetName = String.valueOf(recordMetadata.remove(SHEET_NAME_KEY));
    }

    DelimitedTextReader(char delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    protected void init() {
        try {
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator(delimiter)
                    .withIgnoreQuotations(delimiter != ',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            reader = new CSVReaderBuilder(new FileReader(file))
                    .withCSVParser(parser)
                    .build();
        } catch (FileNotFoundException e) {
            throw new FimsRuntimeException(FileCode.READ_ERROR, 500);
        }

        it = reader.iterator();
        setColumnNames();

        if (!it.hasNext()) {
            throw new FimsRuntimeException(DataReaderCode.NO_DATA, 400, sheetName);
        }

        sheetEntities = config.entitiesForSheet(sheetName);
    }

    private void setColumnNames() {
        // Get the first row to populate Column Names
        try {
            colNames = nextRow();
        } catch (NoSuchElementException e) {
            throw new FimsRuntimeException(DataReaderCode.NO_DATA, 400, sheetName);
        }

        Set<String> colSet = new HashSet<>();

        colNames.set(0, removeBOM(colNames.get(0)));
        for (String col : colNames) {
            if (!colSet.add(col)) {
                throw new FimsRuntimeException(DataReaderCode.DUPLICATE_COLUMNS, 400, sheetName, col);
            }
        }

    }

    /**
     * If you save a csv in excel as utf-8 csv, it will include a BOM as the first byte.
     *
     * Java doesn't detect this and will include it in the value, so we need to manually remove it.
     *
     * https://en.wikipedia.org/wiki/Byte_order_mark
     * @param s
     * @return
     */
    private String removeBOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }

    @Override
    void instantiateRecords() {
        while (it.hasNext()) {
            instantiateRecordsFromRow(nextRow());
        }
    }

    private LinkedList<String> nextRow() {
        if (!it.hasNext())
            throw new NoSuchElementException();

        return new LinkedList<>(Arrays.asList(it.next()));
    }
}
