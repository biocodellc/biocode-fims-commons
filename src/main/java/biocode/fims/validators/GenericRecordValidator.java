package biocode.fims.validators;

import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.renderers.EntityMessages;

/**
 * @author rjewing
 */
public class GenericRecordValidator implements RecordValidator {
    private ProjectConfig config;
    private EntityMessages messages;

    @Override
    public void setProjectConfig(ProjectConfig config) {
        this.config = config;
    }

    @Override
    public boolean validate(RecordSet recordSet) {
        this.messages = new EntityMessages(recordSet.conceptAlias());
        return false;
    }

    @Override
    public boolean hasWarning() {
        return false;
    }

    @Override
    public EntityMessages messages() {
        return messages;
    }
}
