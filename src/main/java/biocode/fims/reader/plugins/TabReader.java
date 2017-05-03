package biocode.fims.reader.plugins;

import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.reader.CharacterCleaner;
import biocode.fims.settings.PathManager;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * //TODO this javadoc needs to be updated. It looks like this file has never had the ability
 * to parse CSV file, only TSV.
 * Provides the ability to parse CSV formatted files.  This implementation is
 * mostly compliant with the RFC 4180 CSV specification.  In particular, 1) each
 * record is expected to be on a single line; 2) fields may be enclosed in
 * double quotes; 3) double quotes inside of a double-quoted string should be
 * escaped with a double quote; 4) space and tab characters are not trimmed from
 * the beginning or end of fields.
 * <p/>
 * This implementation has some additional features and/or limitations that
 * might not be supported by other CSV libraries.  First, if a field is
 * double-quoted, there should be no additional characters outside of the double
 * quotes.  Second, records must not span multiple lines.  Third, standard
 * escape sequences (e.g., "\"\t") are also supported inside of quoted fields.
 * Fourth, blank lines are ignored.
 * <p/>
 * Finally, note that this class expects text to consist of simple ASCII
 * (1 byte) characters, and that characters 0-8 and 10-31 are all treated as
 * empty whitespace and ignored (unless they occur within a quoted string).
 */
public class TabReader extends DelimitedTextReader {
    private static final List<String> EXTS = Arrays.asList("tsv", "txt");
    private static final char DELIMITER = '\t';


    /**
     * This is only to be used for passing the class into the DataReaderFactory
     */
    TabReader() {
        super(DELIMITER);
    }

    /**
     * @param file
     * @param mapping
     * @param recordMetadata must contain the key SHEET_NAME_KEY declaring the {@link Entity#getWorksheet()} of the csv file
     */
    public TabReader(File file, Mapping mapping, RecordMetadata recordMetadata) {
        super(file, mapping, recordMetadata, DELIMITER);
    }

    @Override
    public boolean handlesExtension(String ext) {
        return EXTS.contains(ext.toLowerCase());
    }

    @Override
    void configureTokenizer() {}
}
