package biocode.fims.validation;

import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.renderers.EntityMessages;
import org.springframework.util.Assert;

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
        Assert.notNull(recordSet);

        if (config == null) {
            throw new IllegalStateException("ProjectConfig must not be null. Call setProjectConfig first before validate");
        }

        this.messages = new EntityMessages(recordSet.conceptAlias(), recordSet.entity().getWorksheet());
        return true;
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
