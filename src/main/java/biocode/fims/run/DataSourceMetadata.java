package biocode.fims.run;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.reader.DataReader;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Set;

/**
 * @author rjewing
 */
public class DataSourceMetadata {
    @JsonProperty
    private String dataType;
    @JsonProperty
    private String filename;
    @JsonProperty
    private Map<String, Object> metadata;

    // for jackson
    private DataSourceMetadata() {}

    public DataSourceMetadata(String dataType, String filename, Map<String, Object> metadata) {
        this.dataType = dataType;
        this.filename = filename;
        this.metadata = metadata;
    }

    public String getFilename() {
        return filename;
    }

    public RecordMetadata toRecordMetadata(Set<DataReader.DataReaderType> readerTypes) {
        DataReader.DataReaderType readerType = null;

        for (DataReader.DataReaderType rt: readerTypes) {
            if (rt.type().equalsIgnoreCase(dataType)) {
                readerType = rt;
            }
        }

        if (readerType == null) {
            throw new FimsRuntimeException(DataReaderCode.NOT_FOUND, 400);
        }

        return new RecordMetadata(readerType, metadata);
    }
}
