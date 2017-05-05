package biocode.fims.validators;

import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.renderers.EntityMessages;

/**
 * @author rjewing
 */
public interface RecordValidator {

    void setProjectConfig(ProjectConfig config);

    boolean validate(RecordSet recordSet);

    boolean hasWarning();

    EntityMessages messages();
}