package biocode.fims.reader;

import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.reader.plugins.DataReader;
import biocode.fims.utils.FileUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class DataReaderFactory {
    private final Map<Class<? extends Record>, List<DataReader>> recordTypeReaders;

    public DataReaderFactory(Map<Class<? extends Record>, List<DataReader>> recordTypeReaders) {
        this.recordTypeReaders = recordTypeReaders;
    }

    public DataReader getReader(String filepath, Mapping mapping, RecordMetadata recordMetadata) {
        String ext = FileUtils.getExtension(filepath, "");

        File file = new File(filepath);

        if (!file.exists()) {
            throw new FimsRuntimeException(FileCode.READ_ERROR, 400);
        }

        for (DataReader reader : recordTypeReaders.getOrDefault(recordMetadata.type(), Collections.emptyList())) {

            if (reader.handlesExtension(ext)) {
                return reader.newInstance(file, mapping, recordMetadata);
            }
        }

        throw new FimsRuntimeException(DataReaderCode.NOT_FOUND, 400, ext);
    }
}
