package biocode.fims.reader;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.records.RecordSet;

import java.util.Map;

/**
 * @author rjewing
 */
public class DataConverterFactory {
    private final Map<String, DataConverter> dataConverters;

    public DataConverterFactory(Map<String, DataConverter> dataConverters) {
        this.dataConverters = dataConverters;
    }

    public DataConverter getConverter(String entityType, ProjectConfig projectConfig) {
        return dataConverters.getOrDefault(entityType, new DefaultDataConverter()).newInstance(projectConfig);
    }

    class DefaultDataConverter implements DataConverter {

        @Override
        public void convertRecordSet(RecordSet recordSet, int networkId) {
        }

        @Override
        public DataConverter newInstance(ProjectConfig projectConfig) {
            return new DefaultDataConverter();
        }
    }
}
