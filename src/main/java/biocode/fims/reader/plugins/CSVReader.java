package biocode.fims.reader.plugins;


import biocode.fims.projectConfig.models.Entity;
import biocode.fims.records.RecordMetadata;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReader;

import java.io.*;
import java.util.Collections;
import java.util.List;


/**
 * Provides the ability to parse CSV formatted files.  This implementation is
 * mostly compliant with the RFC 4180 CSV specification.  In particular, 1) each
 * record is expected to be on a single line; 2) fields may be enclosed in
 * double quotes; 3) double quotes inside of a double-quoted string should be
 * escaped with a double quote; 4) space and tab characters are not trimmed from
 * the beginning or end of fields.
 * <p>
 * This implementation has some additional features and/or limitations that
 * might not be supported by other CSV libraries.  First, if a field is
 * double-quoted, there should be no additional characters outside of the double
 * quotes.  Second, records must not span multiple lines.  Third, standard
 * escape sequences (e.g., "\"\t") are also supported inside of quoted fields.
 * Fourth, blank lines are ignored.
 * <p>
 * Finally, note that this class expects text to consist of simple ASCII
 * (1 byte) characters, and that characters 0-8 and 10-31 are all treated as
 * empty whitespace and ignored (unless they occur within a quoted string).
 */
public class CSVReader extends DelimitedTextReader {
    private static final List<String> EXTS = Collections.singletonList("csv");
    private static final char DELIMITER = ',';

    /**
     * This is only to be used for passing the class into the DataReaderFactory
     */
    public CSVReader() {
        super(DELIMITER);
    }

    /**
     * @param file
     * @param projectConfig
     * @param recordMetadata must contain the key SHEET_NAME_KEY declaring the {@link Entity#getWorksheet()} of the csv file
     */
    public CSVReader(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        super(file, projectConfig, recordMetadata, DELIMITER);
    }

    @Override
    public boolean handlesExtension(String ext) {
        return EXTS.contains(ext.toLowerCase());
    }

    @Override
    public DataReader newInstance(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        return new CSVReader(file, projectConfig, recordMetadata);
    }
}
