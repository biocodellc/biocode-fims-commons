package biocode.fims.reader;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.records.RecordSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class DataConverterFactory {
    private final Map<String, List<DataConverter>> dataConverters;

    public DataConverterFactory(Map<String, List<DataConverter>> dataConverters) {
        this.dataConverters = dataConverters;
    }

    public List<DataConverter> getConverters(String entityType, ProjectConfig projectConfig) {
        return dataConverters.getOrDefault(
                entityType,
                Collections.singletonList(new DefaultDataConverter())
        )
                .stream()
                .map(d -> d.newInstance(projectConfig))
                .collect(Collectors.toList());
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
