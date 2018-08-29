package biocode.fims.reader;

import biocode.fims.records.RecordSet;
import biocode.fims.config.project.ProjectConfig;

/**
 * @author rjewing
 */
public interface DataConverter {

    RecordSet convertRecordSet(RecordSet recordSet, int networkId, String expeditionCode);

    DataConverter newInstance(ProjectConfig projectConfig);
}

