package biocode.fims.reader;

import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;

import java.io.File;
import java.util.List;

/**
 * @author rjewing
 */
public interface DataReader {

    List<RecordSet> getRecordSets(int projectId, String expeditionCode);

    boolean handlesExtension(String ext);

    DataReader newInstance(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata);

    DataReaderType readerType();

    class DataReaderType {
        private final String type;

        public DataReaderType(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DataReaderType)) return false;

            DataReaderType that = (DataReaderType) o;

            return type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }
    }
}
