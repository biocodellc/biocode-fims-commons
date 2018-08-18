package biocode.fims.reader;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.records.RecordMetadata;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.utils.FileUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rjewing
 */
public class DataReaderFactory {
    private Map<DataReader.DataReaderType, List<DataReader>> dataReaders;

    public DataReaderFactory(Map<DataReader.DataReaderType, List<DataReader>> dataReaders) {
        this.dataReaders = dataReaders;
    }

    public DataReader getReader(String filepath, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        String ext = FileUtils.getExtension(filepath, "");

        File file = new File(filepath);

        if (!file.exists()) {
            throw new FimsRuntimeException(FileCode.READ_ERROR, 400);
        }

        for (DataReader reader : dataReaders.getOrDefault(recordMetadata.readerType(), Collections.emptyList())) {

            if (reader.handlesExtension(ext)) {
                return reader.newInstance(file, projectConfig, recordMetadata);
            }
        }

        throw new FimsRuntimeException(DataReaderCode.NOT_FOUND, 400, ext);
    }

    public Set<DataReader.DataReaderType> getReaderTypes() {
        return dataReaders.keySet();
    }
}
