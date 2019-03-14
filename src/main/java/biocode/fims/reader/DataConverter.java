package biocode.fims.reader;

import biocode.fims.records.RecordSet;
import biocode.fims.config.project.ProjectConfig;

/**
 * Interface to allow modification of {@link RecordSet} after creation from a datafile.
 * <p>
 * This is the last step in the DatasetBuilder process thus the RecordSet will be fully
 * populated. The parent() will have been set
 *
 * @author rjewing
 */
public interface DataConverter {

    /**
     * Mutate the RecordSet as needed.
     * <p>
     * Note: Implementations should not mutate the Record's directly, but
     * should instead mutate the Record.clone(), add the new record, and
     * remove the old.
     *
     * @param recordSet
     * @param networkId
     * @return
     */
    void convertRecordSet(RecordSet recordSet, int networkId);

    DataConverter newInstance(ProjectConfig projectConfig);
}

