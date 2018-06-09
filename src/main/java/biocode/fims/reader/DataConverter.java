package biocode.fims.reader;

import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;

/**
 * @author rjewing
 */
public interface DataConverter {

    RecordSet convertRecordSet(RecordSet recordSet, int projectId, String expeditionCode);

    DataConverter newInstance(ProjectConfig projectConfig);
}

