package biocode.fims.reader.plugins;

import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.records.RecordMetadata;
import biocode.fims.reader.DataReader;

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
    public TabReader() {
        super(DELIMITER);
    }

    /**
     * @param file
     * @param projectConfig
     * @param recordMetadata must contain the key SHEET_NAME_KEY declaring the {@link Entity#getWorksheet()} of the csv file
     */
    public TabReader(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        super(file, projectConfig, recordMetadata, DELIMITER);
    }

    @Override
    public boolean handlesExtension(String ext) {
        return EXTS.contains(ext.toLowerCase());
    }


    @Override
    public DataReader newInstance(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        return new TabReader(file, projectConfig, recordMetadata);
    }
}
