package biocode.fims.reader.plugins;

import biocode.fims.digester.Mapping;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.models.records.RecordSet;

import java.io.File;
import java.util.List;

/**
 * @author rjewing
 */
public interface DataReader {

    List<RecordSet> getRecordSets();

    boolean handlesExtension(String ext);

    DataReader newInstance(File file, Mapping mapping, RecordMetadata recordMetadata);
}
