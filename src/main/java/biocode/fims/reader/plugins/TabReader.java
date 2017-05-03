package biocode.fims.reader.plugins;

import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.models.records.RecordMetadata;

import java.io.*;
import java.util.Arrays;
import java.util.List;


/**
 * Provides the ability to parse Tab delimited files.
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
