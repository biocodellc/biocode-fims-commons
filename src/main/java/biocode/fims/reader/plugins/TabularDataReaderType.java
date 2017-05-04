package biocode.fims.reader.plugins;

/**
 * @author rjewing
 */
public class TabularDataReaderType {
    public static final String READER_TYPE_STRING = "TABULAR";
    public static final DataReader.DataReaderType READER_TYPE = new DataReader.DataReaderType(READER_TYPE_STRING);

    private TabularDataReaderType() {}
}
